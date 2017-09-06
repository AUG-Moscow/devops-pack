package ru.augspb.plugins.jira.workflow.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atlassian.jira.issue.Issue;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Validator;
import com.opensymphony.workflow.InvalidInputException;
import java.util.Map;

public class CommitValidator implements Validator
{
    private static final Logger log = LoggerFactory.getLogger(CommitValidator.class);
    public static final String FIELD_WORD = "word";

    public void validate(Map transientVars, Map args, PropertySet ps) throws InvalidInputException
    {
        String word = (String)transientVars.get(FIELD_WORD);
        Issue issue = (Issue)transientVars.get("issue");
        String description = issue.getDescription();

        if (description == null || !description.contains(word)) {
            throw new InvalidInputException("Issue must contain the word '" + word + "' in the description");
        }
    }
}
