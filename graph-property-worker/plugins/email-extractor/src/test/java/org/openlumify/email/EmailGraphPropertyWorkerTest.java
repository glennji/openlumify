package org.openlumify.email;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlumify.core.ingest.graphProperty.GraphPropertyWorker;
import org.openlumify.core.ingest.graphProperty.TermMentionGraphPropertyWorkerTestBase;

import java.util.Arrays;

import static org.openlumify.email.EmailGraphPropertyWorker.EMAIL_CONCEPT_INTENT;

@RunWith(MockitoJUnitRunner.class)
public class EmailGraphPropertyWorkerTest extends TermMentionGraphPropertyWorkerTestBase {
    private static final String EMAIL_TEXT = "This person's email is person.one@openlumify.com, and his best buddy's email @ vertexium.org is person.two@vertexium.org";
    private static final String EMAIL_NEW_LINES = "This person's email is \nperson.one@openlumify.com, and his best buddy's \nemail is person.two@vertexium.org\n";
    private static final String EMAIL_MISSING = "This is a sentence without any emails in it.";

    @Before
    public void setup() {
        addConceptWithIntent(CONCEPT_IRI, EMAIL_CONCEPT_INTENT);
    }

    @Override
    public GraphPropertyWorker getGpw() throws Exception {
        return new EmailGraphPropertyWorker();
    }

    @Test
    public void testEmailExtraction() throws Exception {
        doExtractionTest(EMAIL_TEXT, Arrays.asList(
                new ExpectedTermMention("person.one@openlumify.com", 23L, 45L),
                new ExpectedTermMention("person.two@vertexium.org", 93L, 117L)
        ));
    }

    @Test
    public void testEmailExtractionWithNewlines() throws Exception {
        doExtractionTest(EMAIL_NEW_LINES, Arrays.asList(
                new ExpectedTermMention("person.one@openlumify.com", 24L, 46L),
                new ExpectedTermMention("person.two@vertexium.org", 79L, 103L)
        ));
    }

    @Test
    public void testNegativeEmailExtraction() throws Exception {
        doExtractionTest(EMAIL_MISSING, null);
    }
}
