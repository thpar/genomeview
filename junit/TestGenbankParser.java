/**
 * %HEADER%
 */
package junit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.SortedSet;

import net.sf.jannot.Entry;
import net.sf.jannot.Feature;
import net.sf.jannot.Location;
import net.sf.jannot.parser.GenbankParser;

import org.junit.Assert;
import org.junit.Test;

public class TestGenbankParser {

    @Test
    public void testGenbankParser() {
        GenbankParser parser = new GenbankParser();
        try {
            Entry[] entries = parser.parse(new FileInputStream("sequences.gb"), null);

            // System.out.println("ACC:" +
            // entries[0].description.getAccessionNumbers());
            List<Feature> fs = entries[0].annotation.getAll();
            for (Feature f : fs) {
                // System.out.println(f.type());
                // System.out.println(f.location());
                SortedSet<Location> loc = f.location();
                // for (Location l : loc)
                // System.out.println("\t" + l.getParent());
            }
            System.out.println(entries[0].sequence);

            Assert.assertTrue(entries[0].sequence.toString().startsWith(
                    "actaccgctatcaatatactcccacaaatatcaagagccttcccagtattaaatttgcta"));
            Assert.assertTrue(entries[0].sequence.toString().endsWith("gatcac"));

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
