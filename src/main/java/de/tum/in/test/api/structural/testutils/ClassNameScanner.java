package de.tum.in.test.api.structural.testutils;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 5.0 (11.11.2020)
 *
 * This class scans the submission project if the current expected class is actually
 * present in it or not. The result is returned as an instance of ScanResult.
 * The ScanResult consists of a ScanResultType and a ScanResultMessage as a string.
 * ScanResultType is an enum and is implemented so that identifying just the type of
 * the error and the binding of several messages to a certain result is possible.
 *
 * There are the following possible results:
 * - The class has the correct name and is placed in the correct package.
 * - The class has the correct name but is misplaced.
 * - The class name has wrong casing, but is placed in the correct package.
 * - The class name has wrong casing and is misplaced.
 * - The class name has typos, but is placed in the correct package.
 * - The class name has typos and is misplaced.
 * - The class name has too many typos, thus is declared as not found.
 * - Undefined, which is used to initialize the scan result.
 *
 *  A note on the limit of allowed number of typos: the maximal number depends
 *  on the length of the class name and is defined as ceiling(classNameLength / 4).
 */
public class ClassNameScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ClassNameScanner.class);

    private static final String DEVIATES_FROM_THE_EXPECTATION = ", which deviates from the expectation.";
    private static final String IMPLEMENTED_A_CLASS = "We found that you implemented a class ";
    private static final String THE_CLASS = "The class ";
    private static final String EXPECTS_CLASS_WITH_NAME = "The exercise expects a class with the name ";
    private static final String IN_THE_PACKAGE = " in the package ";
    private static final String CORRECT_NAME = " has the correct name";

    // The class name and package name of the expected class that is currently being searched after.
    private final String expectedClassName;
    private final String expectedPackageName;

    // The names of the classes observed in the project
    private final Map<String, List<String>> observedClasses;
    private final ScanResult scanResult;

    public ClassNameScanner(String expectedClassName, String expectedPackageName) {
        this.expectedClassName = expectedClassName;
        this.expectedPackageName = expectedPackageName;
        this.observedClasses = retrieveObservedClasses();
        this.scanResult = computeScanResult();
    }

    public ScanResult getScanResult() {
        return this.scanResult;
    }

    /**
     * This method computes the scan result of the submission for the expected class name.
     * It first checks if the class is in the project at all.
     * If that's the case, it then checks if that class is properly placed or not and generates feedback accordingly.
     * Otherwise the method loops over the observed classes and checks if any of the observed classes is actually the
     * expected one but with the wrong case or types in the name.
     * It again checks in each case if the class is misplaced or not and delivers the feedback.
     * Finally, in none of these holds, the class is simply declared as not found.
     *
     * @return An instance of ScanResult containing the result type and the feedback message.
     */
    private ScanResult computeScanResult() {
        // Initialize the type and the message of the scan result.
        ScanResultType scanResultType = ScanResultType.UNDEFINED;
        String scanResultMessage;

        boolean classIsFound = observedClasses.containsKey(expectedClassName);
        boolean classIsCorrectlyPlaced;
        boolean classIsPresentMultipleTimes;

        String foundObservedClassName = null;
        String foundObservedPackageName = null;

        if(classIsFound) {
            List<String> observedPackageNames = observedClasses.get(expectedClassName);
            classIsPresentMultipleTimes = observedPackageNames.size() > 1;
            classIsCorrectlyPlaced = !classIsPresentMultipleTimes && (observedPackageNames.contains(expectedPackageName));

            if (classIsPresentMultipleTimes) {
                scanResultType = ScanResultType.CORRECT_NAME_MULTIPLE_TIMES_PRESENT;
            }
            else {
                scanResultType = classIsCorrectlyPlaced ? ScanResultType.CORRECT_NAME_CORRECT_PLACE : ScanResultType.CORRECT_NAME_MISPLACED;
            }

            foundObservedClassName = expectedClassName;
            foundObservedPackageName = observedPackageNames.toString();
        }
        else {
            for(var observedClass : observedClasses.entrySet()) {
                var observedClassName = observedClass.getKey();
                var observedPackageNames = observedClass.getValue();
                classIsPresentMultipleTimes = observedPackageNames.size() > 1;
                classIsCorrectlyPlaced = !classIsPresentMultipleTimes && (observedPackageNames.contains(expectedPackageName));

                boolean hasWrongCase = observedClassName.equalsIgnoreCase(expectedClassName);
                boolean hasTypos = FuzzySearch.ratio(observedClassName, expectedClassName) > 90;

                foundObservedClassName = observedClassName;
                foundObservedPackageName = observedPackageNames.toString();

                if(hasWrongCase) {
                    if (classIsPresentMultipleTimes) {
                        scanResultType = ScanResultType.WRONG_CASE_MULTIPLE_TIMES_PRESENT;
                    }
                    else {
                        scanResultType = classIsCorrectlyPlaced ? ScanResultType.WRONG_CASE_CORRECT_PLACE : ScanResultType.WRONG_CASE_MISPLACED;
                    }
                    break;
                } else if(hasTypos) {
                    if (classIsPresentMultipleTimes) {
                        scanResultType = ScanResultType.TYPOS_MULTIPLE_TIMES_PRESENT;
                    }
                    else {
                        scanResultType = classIsCorrectlyPlaced ? ScanResultType.TYPOS_CORRECT_PLACE : ScanResultType.TYPOS_MISPLACED;
                    }
                    break;
                } else {
                    scanResultType = ScanResultType.NOTFOUND;
                }
            }
        }

        switch (scanResultType) {
            case CORRECT_NAME_CORRECT_PLACE:
                scanResultMessage = THE_CLASS + foundObservedClassName + CORRECT_NAME + " and is in the correct package.";
                break;
            case CORRECT_NAME_MISPLACED:
                scanResultMessage = THE_CLASS + foundObservedClassName + CORRECT_NAME + ","
                        + " but the package it's in, " + foundObservedPackageName + ", deviates from the expectation."
                        + "  Make sure it is placed in the correct package.";
                break;
            case CORRECT_NAME_MULTIPLE_TIMES_PRESENT:
                scanResultMessage = THE_CLASS + foundObservedClassName + CORRECT_NAME + ","
                        + " but it is located multiple times in the project and in the packages: "
                        + foundObservedPackageName + DEVIATES_FROM_THE_EXPECTATION
                        + " Make sure to place the class in the correct package and remove any superfluous ones.";
                break;
            case WRONG_CASE_CORRECT_PLACE:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for wrong upper case / lower case lettering.";
                break;
            case WRONG_CASE_MISPLACED:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + IN_THE_PACKAGE + expectedPackageName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + "," + IN_THE_PACKAGE + foundObservedPackageName
                        + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for wrong upper case / lower case lettering and make sure you place it in the correct package.";
                break;
            case WRONG_CASE_MULTIPLE_TIMES_PRESENT:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + IN_THE_PACKAGE + expectedPackageName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + "," + IN_THE_PACKAGE + foundObservedPackageName
                        + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for wrong upper case / lower case lettering and make sure you place one class in the correct package and remove any superfluous classes.";
                break;
            case TYPOS_CORRECT_PLACE:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for typos in the class name.";
                break;
            case TYPOS_MISPLACED:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + IN_THE_PACKAGE + expectedPackageName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + "," + IN_THE_PACKAGE + foundObservedPackageName
                        + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for typos in the class name and make sure you place it in the correct package.";
                break;
            case TYPOS_MULTIPLE_TIMES_PRESENT:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + IN_THE_PACKAGE + expectedPackageName + ". "
                        + IMPLEMENTED_A_CLASS + foundObservedClassName + "," + IN_THE_PACKAGE + observedClasses.get(foundObservedClassName).toString()
                        + DEVIATES_FROM_THE_EXPECTATION
                        + " Check for typos in the class name and make sure you place one class it in the correct package and remove any superfluous classes.";
                break;
            case NOTFOUND:
                scanResultMessage = EXPECTS_CLASS_WITH_NAME + expectedClassName + IN_THE_PACKAGE + expectedPackageName
                        + ". You did not implement the class in the exercise.";
                break;
            default:
                scanResultMessage = "The class could not be scanned.";
                break;
        }

        return new ScanResult(scanResultType, scanResultMessage);
    }

    /**
     * This method retrieves the actual type names and their packages by walking the project file structure.
     * The root node (which is the assignment folder) is defined in the pom.xml file of the project.
     * @return The map containing the type names as keys and the type packages as values.
     */
    private Map<String, List<String>> retrieveObservedClasses() {
        Map<String, List<String>> observedTypes = new HashMap<>();

        try {
            File pomFile = new File("pom.xml");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            // make sure to avoid loading external files which would not be compliant
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document pomXmlDocument = documentBuilder.parse(pomFile);

            NodeList buildNodes = pomXmlDocument.getElementsByTagName("build");
            for(int i = 0; i < buildNodes.getLength(); i++) {
                Node buildNode = buildNodes.item(i);
                if(buildNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element buildNodeElement = (Element) buildNode;
                    String sourceDirectoryPropertyValue = buildNodeElement.getElementsByTagName("sourceDirectory").item(0).getTextContent();
                    String assignmentFolderName = sourceDirectoryPropertyValue.substring(sourceDirectoryPropertyValue.indexOf("}") + 2);
                    walkProjectFileStructure(assignmentFolderName, new File(assignmentFolderName), observedTypes);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Could not retrieve the source directory from the pom.xml file. Contact your instructor.", e);
        }

        return observedTypes;
    }

    /**
     * This method recursively walks the actual folder file structure starting from the assignment folder and adds
     * each type it finds e.g. filenames ending with .java and .kt to the passed JSON object.
     * @param assignmentFolderName: The root folder where the method starts walking the project structure.
     * @param node: The current node the method is visiting.
     * @param foundTypes: The JSON object where the type names and packages get appended.
     */
    private void walkProjectFileStructure(String assignmentFolderName, File node, Map<String, List<String>> foundTypes) {
        String fileName = node.getName();

        if(fileName.endsWith(".java") || fileName.endsWith(".kt")) {
            String[] fileNameComponents = fileName.split("\\.");
            String fileExtension = fileNameComponents[fileNameComponents.length - 1];

            String className = fileNameComponents[fileNameComponents.length - 2];
            String packageName = node.getPath().substring(0, node.getPath().indexOf(fileExtension));
            packageName = packageName.substring(
                    packageName.indexOf(assignmentFolderName) + assignmentFolderName.length() + 1,
                    packageName.lastIndexOf(File.separator + className));
            packageName = packageName.replace(File.separatorChar, '.');

            if (packageName.charAt(0) == '.') {
                packageName = packageName.substring(1);
            }

            if(foundTypes.containsKey(className)) {
                foundTypes.get(className).add(packageName);
            }
            else {
                foundTypes.put(className, Collections.singletonList(packageName));
            }
        }

        if(node.isDirectory()) {
            String[] subNodes = node.list();
            if (subNodes != null && subNodes.length > 0) {
                for (String currentSubNode : subNodes) {
                    walkProjectFileStructure(assignmentFolderName, new File(node, currentSubNode), foundTypes);
                }
            }
        }
    }
}