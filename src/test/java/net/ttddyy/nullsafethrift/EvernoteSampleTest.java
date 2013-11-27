package net.ttddyy.nullsafethrift;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.edam.type.Resource;
import org.junit.Test;

import static net.ttddyy.nullsafethrift.ThriftWrapper.w;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Tadaya Tsuyukubo
 */
public class EvernoteSampleTest {

    @Test
    public void testWrap() throws Exception {
        Note note = new Note();
        note.setAttributes(new NoteAttributes());
        assertThat(note.getTagGuids(), is(nullValue()));
        assertThat(note.getAttributes().getClassifications(), is(nullValue()));

        Note wrapped = w(note);
        assertThat(wrapped.getTagGuids(), is(emptyCollectionOf(String.class)));
        assertThat(wrapped.getAttributes().getClassifications(), is(notNullValue()));
        assertThat(wrapped.getAttributes().getClassifications().size(), is(0));

    }

    @Test(expected = NullPointerException.class)
    public void testLoop() throws Exception {
        Note note = new Note();
        for (Resource resource : note.getResources()) {
        }
        fail();
    }

    @Test
    public void testSafeLoop() throws Exception {
        Note wrapped = w(new Note());  // wrapped
        for (Resource resource : wrapped.getResources()) {
        }
    }

}
