/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.d4ts;

import dap4.ce.CEConstraint;
import dap4.ce.parser.*;
import dap4.core.dmr.*;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.*;
import dap4.dap4shared.*;
import dap4.servlet.*;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class D4TSServlet extends DapServlet
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    static final boolean PARSEDEBUG = false;

    static final String TESTDATADIR = "testfiles";

    //////////////////////////////////////////////////
    // Instance variables

    //////////////////////////////////////////////////
    // Constructor(s)

    public D4TSServlet()
    {
        super();
    }

    //////////////////////////////////////////////////////////
    // doXXX Methods

    //////////////////////////////////////////////////////////
    // Capabilities processors

    /**
     * Process a capabilities request.
     * Currently, generate the front page.
     *
     * @param drq The merged dap state
     */

    protected void
    doCapabilities(DapRequest drq)
        throws IOException
    {
        // Get the url + servlet path so we can reuse for constructing front page

        // Get the complete url used to get to this point
        String url = DapUtil.canonicalpath(drq.getURL().toString(), false);

        addCommonHeaders(drq);

        // Figure out the directory containing
        // the files to display.
        String dir = getResourceFile(drq, TESTDATADIR, true);
        if(dir == null)
            throw new DapException("Cannot locate resources directory");

        // Generate the front page
        FrontPage front = new FrontPage(dir, url);
        String frontpage = front.buildPage();

        if(frontpage == null)
            throw new DapException("Cannot create front page")
                .setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // // Convert to UTF-8 and then to byte[]
        byte[] frontpage8 = DapUtil.extract(DapUtil.UTF8.encode(frontpage));

        OutputStream out = drq.getOutputStream();
        out.write(frontpage8);

    }

    //////////////////////////////////////////////////////////
    // Extension processors

    /**
     * Process a .ser
     *
     * @param drq         The merged dap state
     * @param datasetname The path of the dataset whose DMR is to be returned.
     * @param mode        The extension of what is to be read
     */

    void
    doSynthetic(DapRequest drq, String datasetname, RequestMode mode)
        throws IOException
    {
        //assert datasetname.endsWith(SYNTHETICEXT);
        addCommonHeaders(drq);// Add relevant headers

        // Read the .syn file (which is simply a DMR)
        String datasetpath = getResourceFile(drq, datasetname, false);
        FileInputStream stream = new FileInputStream(datasetpath);
        String document = DapUtil.readtextfile(stream);

        // Since we may be applying constraints,
        // we need to parse the dmr.
        Dap4Parser pushparser = new Dap4Parser(new DapFactoryDMR());
        if(PARSEDEBUG)
            pushparser.setDebugLevel(1);
        try {
            if(!pushparser.parse(document))
                throw new DapException("DMR Parse failed");
        } catch (SAXException se) {
            throw new DapException(se);
        }
        if(pushparser.getErrorResponse() != null)
            throw new DapException("Error Response Document not supported");
        DapDataset dmr = pushparser.getDMR();

        // Process any constraint
        CEConstraint ce = null;
        String sce = drq.queryLookup(DapProtocol.CONSTRAINTTAG);
        ce = buildconstraint(drq, sce, dmr);

        OutputStream out = drq.getOutputStream();
        // Wrap the outputstream with a Chunk writer
        ChunkWriter cw = new ChunkWriter(out, mode, this.byteorder);

        switch (mode) {
        case DMR:
            // Provide a PrintWriter for capturing the DMR.
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            // Get the DMR as a string
            DMRPrint dapprinter = new DMRPrint(pw);
            dapprinter.printDMR(ce);
            pw.close();
            sw.close();
            cw.writeDMR(sw.toString()); // Dump just the DMR
            break;
        case DAP:
            Generator generator = new Generator(Value.ValueSource.RANDOM);
            generator.generate(dmr, ce, cw);
            break;
        default:
            throw new DapException("Unexpected request mode: " + mode);
        }
        cw.close();
    }

}

