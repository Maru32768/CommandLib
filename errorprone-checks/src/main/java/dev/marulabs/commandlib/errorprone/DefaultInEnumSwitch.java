package dev.marulabs.commandlib.errorprone;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.code.Type;

import javax.lang.model.element.ElementKind;
import java.util.List;

/**
 * Rejects a {@code default} in a switch on an enum. Without one, Error Prone's {@code MissingCasesInEnumSwitch}
 * reports a switch statement that misses a constant, and javac a switch expression, so a constant added later cannot
 * slip into a branch written for the others. Java 11 has no switch expressions, so there the statements carry it.
 */
@BugPattern(summary = "List every constant of the enum instead of a default, and throw after a switch statement", severity = BugPattern.SeverityLevel.ERROR)
public final class DefaultInEnumSwitch extends BugChecker implements BugChecker.SwitchTreeMatcher, BugChecker.SwitchExpressionTreeMatcher {
    @Override
    public Description matchSwitch(SwitchTree tree, VisitorState state) {
        return match(tree.getExpression(), tree.getCases());
    }

    @Override
    public Description matchSwitchExpression(SwitchExpressionTree tree, VisitorState state) {
        return match(tree.getExpression(), tree.getCases());
    }

    private Description match(ExpressionTree selector, List<? extends CaseTree> cases) {
        Type type = ASTHelpers.getType(selector);
        if (type == null || type.asElement()
                                .getKind() != ElementKind.ENUM) {
            return Description.NO_MATCH;
        }
        for (CaseTree caseTree : cases) {
            if (ASTHelpers.isSwitchDefault(caseTree)) {
                return describeMatch(caseTree);
            }
        }
        return Description.NO_MATCH;
    }
}
