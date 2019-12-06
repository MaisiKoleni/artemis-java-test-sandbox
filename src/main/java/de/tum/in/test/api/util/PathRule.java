package de.tum.in.test.api.util;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Objects;

import de.tum.in.test.api.BlacklistPath;
import de.tum.in.test.api.PathActionLevel;
import de.tum.in.test.api.PathType;
import de.tum.in.test.api.WhitelistPath;

public final class PathRule {
	private final RuleType ruleType;
	private final PathType pathType;
	private final PathActionLevel actionLevel;
	private final String pathPattern;
	private final PathMatcher pathMatcher;

	private PathRule(RuleType ruleType, PathType pathType, PathActionLevel actionLevel, String pathPattern) {
		this.ruleType = ruleType;
		this.pathType = pathType;
		this.actionLevel = actionLevel;
		this.pathPattern = pathPattern;
		this.pathMatcher = this.pathType.convertToPathMatcher(this.pathPattern);
	}

	public RuleType getRuleType() {
		return ruleType;
	}

	public PathType getPathType() {
		return pathType;
	}

	public PathActionLevel getActionLevel() {
		return actionLevel;
	}

	public String getPathPattern() {
		return pathPattern;
	}

	public PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	public boolean matchesWithLevel(Path path, PathActionLevel request) {
		if (ruleType == RuleType.BLACKLIST)
			return request.isAboveOrEqual(actionLevel) && pathMatcher.matches(path);
		return request.isBelowOrEqual(actionLevel) && pathMatcher.matches(path);
	}

	@Override
	public String toString() {
		return String.format("PathRule[\"%s\" (%s) in %s; level %s]", pathPattern, pathType, ruleType, actionLevel);
	}

	@Override
	public int hashCode() {
		return Objects.hash(actionLevel, pathPattern, pathType, ruleType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PathRule))
			return false;
		PathRule other = (PathRule) obj;
		return actionLevel == other.actionLevel && pathType == other.pathType && ruleType == other.ruleType
				&& Objects.equals(pathPattern, other.pathPattern);
	}

	enum RuleType {
		WHITELIST,
		BLACKLIST;
	}

	public static PathRule of(WhitelistPath whitelistedPath) {
		return new PathRule(RuleType.WHITELIST, whitelistedPath.type(), whitelistedPath.level(),
				whitelistedPath.value());
	}

	public static PathRule of(BlacklistPath blacklistedPath) {
		return new PathRule(RuleType.BLACKLIST, blacklistedPath.type(), blacklistedPath.level(),
				blacklistedPath.value());
	}
}