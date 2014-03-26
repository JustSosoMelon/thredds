/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.test.util;

import junit.framework.TestCase;
import dap4.core.util.DapUtil;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class UnitTestCommon extends TestCase
{
    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = true;

    static protected final Charset UTF8 = Charset.forName("UTF-8");

    static final String DEFAULTTREEROOT = "dap4";

    static public final String FILESERVER = "dap4:file://";

    // NetcdfDataset enhancement to use: need only coord systems
    static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

    static public final String CONSTRAINTTAG = "dap4.ce";

    // Order is important; testing reachability is in the order
    // listed
    static public final Source[] SOURCES = new Source[]{
            new Source("remotetest", false,
                    "http://remotetest.unidata.ucar.edu/d4ts",
                    "dap4://remotetest.unidata.ucar.edu/d4ts"),
            new Source("localhost", false,
                    "http://localhost:8080/d4ts",
                    "dap4://localhost:8080/d4ts"),
            new Source("file", true, null, FILESERVER),
    };

    //////////////////////////////////////////////////
    // Type Declarations

    static public class Source
    {
        public String name;
        public String testurl;
        public String prefix;
        public boolean isfile;

        public Source(String name, boolean isfile, String testurl, String prefix)
        {
            this.name = name;
            this.testurl = testurl;
            this.prefix = prefix;
            this.isfile = isfile;
        }
    }

    //////////////////////////////////////////////////
    // Static variables

    static public org.slf4j.Logger log;

    // Define a tree pattern to recognize the root.
    static String patternroot = DEFAULTTREEROOT; // dir to locate
    static String dap4Root = null;

    static {
        // Compute the root path
        dap4Root = locateDAP4Root();
    }

    //////////////////////////////////////////////////
    // static methods

    static public String getDAP4Root()
    {
        return dap4Root;
    }

    static void setTreePattern(String root, String[] subdirs)
    {
        patternroot = root;
    }

    // Walk around the directory structure to locate
    // the path to the thredds root

    static String
    locateDAP4Root()
    {
        // Walk up the user.dir path looking for a node that has
        // the name of the DEFAULTTREEROOT 

        String path = System.getProperty("user.dir");

        if (DEBUG) {
            System.err.println("user.dir=" + path);
            System.err.flush();
        }

        // clean up the path
        path = DapUtil.canonicalpath(path, false);

        File prefix = new File(path);
        for (; prefix != null; prefix = prefix.getParentFile()) {//walk up the tree
            if (patternroot.equals(prefix.getName())) try {
		return DapUtil.canonicalpath(prefix.getCanonicalPath(), false);
            } catch (IOException ioe) {};
        }
        return null;
    }

    static protected String
    rebuildpath(String[] pieces, int last)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i <= last; i++) {
            buf.append("/");
            buf.append(pieces[i]);
        }
        return buf.toString();
    }

    static public void
    clearDir(File dir, boolean clearsubdirs)
    {
        // wipe out the dir contents
        if (!dir.exists()) return;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (clearsubdirs) {
                    clearDir(f, true); // clear subdirs
                    f.delete();
                }
            } else
                f.delete();
        }
    }

    //////////////////////////////////////////////////
    // Instance databuffer

    protected String title = "Testing";

    public UnitTestCommon()
    {
        this("UnitTest");
    }

    public UnitTestCommon(String name)
    {
        super(name);
        this.title = name;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return this.title;
    }

    // Copy result into the a specified dir
    public void
    writefile(String path, String content)
            throws IOException
    {
        FileWriter out = new FileWriter(path);
        out.write(content);
        out.close();
    }

    // Copy result into the a specified dir
    static public void
    writefile(String path, byte[] content)
            throws IOException
    {
        FileOutputStream out = new FileOutputStream(path);
        out.write(content);
        out.close();
    }

    static public String
    readfile(String filename)
            throws IOException
    {
        StringBuilder buf = new StringBuilder();
        FileReader file = new FileReader(filename);
        BufferedReader rdr = new BufferedReader(file);
        String line;
        while ((line = rdr.readLine()) != null) {
            if (line.startsWith("#")) continue;
            buf.append(line + "\n");
        }
        return buf.toString();
    }

    static public byte[]
    readbinaryfile(String filename)
            throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FileInputStream file = new FileInputStream(filename);
        return DapUtil.readbinaryfile(file);
    }

    public void
    visual(String header, String captured)
    {
        if (!captured.endsWith("\n"))
            captured = captured + "\n";
        // Dump the output for visual comparison
        System.out.println("Testing " + getName() + ": " + header + ":");
        System.out.println("---------------");
        System.out.print(captured);
        System.out.println("---------------");
    }

    public boolean
    compare(String baselinecontent, String testresult)
            throws Exception
    {
        StringReader baserdr = new StringReader(baselinecontent);
        StringReader resultrdr = new StringReader(testresult);
        // Diff the two files
        Diff diff = new Diff("Testing " + getTitle());
        boolean pass = !diff.doDiff(baserdr, resultrdr);
        baserdr.close();
        resultrdr.close();
        return pass;
    }

    // Properly access a dataset
    static public NetcdfDataset openDataset(String url)
            throws IOException
    {
        return NetcdfDataset.acquireDataset(null, url, ENHANCEMENT, -1, null, null);
    }

    // Fix up a filename reference in a string
    static public String shortenFileName(String text, String filename)
    {
        // In order to achieve diff consistentcy, we need to
        // modify the output to change "netcdf .../file.nc {...}"
        // to "netcdf file.nc {...}"
        String fixed = filename.replace('\\', '/');
        String shortname = filename;
        if (fixed.lastIndexOf('/') >= 0)
            shortname = filename.substring(fixed.lastIndexOf('/') + 1, filename.length());
        text = text.replaceAll(filename, shortname);
        return text;
    }

    static public void
    tag(String t)
    {
        System.err.println(t);
        System.err.flush();
    }

}

