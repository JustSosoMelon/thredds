package ucar.unidata.test.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Manage the test data directories
 *
 * @author caron
 * @since 3/23/12
 */
public class TestDir {

  /**
   * Old test data directory. may have cruft in it
   * Unidata "//fileserver/data/testdata2" directory.
   */
  private static String testdataDir = null;

  /**
   * New test data directory. do not put temprory files in here. migrate all test data here eventually
   * Unidata "//fileserver/data/testdata2/cdmUnitTest" directory.
   */
  public static String cdmUnitTestDir = null;

  /**
   * Level 1 test data directory (distributed with code and MAY be used in Unidata nightly testing).
   */
  public static String cdmLocalTestDataDir = "src/test/data/";

  /**
   * Temporary data directory (for writing temporary data).
   */
  public static String temporaryLocalDataDir = "target/test/tmp/";

  //////////////////////////////////////////////////////////////////////
  /** Property name for the path to the Unidata test data directory,
   * e.g unidata.testdata2.path=//shemp/data/testdata2/
   * the real directory is at shemp:/data/testdata2
   */
  private static String testdataDirPropName ="unidata.testdata.path";


  /** Filename of the user property file read from the "user.home" directory
   * if the "unidata.testdata2.path" and "unidata.upc.share.path" are not
   * available as system properties. */
  private static String threddsPropFileName = "thredds.properties";

  // TODO all the static stuff below is also in thredds.unidata.testUtil.TestAll, can we unify?

  // Determine how Unidata "/upc/share" directory is mounted
  // on local machine by reading system or THREDDS property.
  static {
    // Check for system property
    String testdataDirPath = System.getProperty( testdataDirPropName );

    if (testdataDirPath == null )
    {
      // Get user property.
      File userHomeDirFile = new File( System.getProperty( "user.home" ) );
      File userThreddsPropsFile = new File( userHomeDirFile, threddsPropFileName );
      if ( userThreddsPropsFile.exists() && userThreddsPropsFile.canRead() )
      {
        Properties userThreddsProps = new Properties();
        try
        {
          userThreddsProps.load( new FileInputStream( userThreddsPropsFile ) );
        }
        catch ( IOException e )
        {
          System.out.println( "**Failed loading user THREDDS property file: " + e.getMessage() );
        }
        if ( userThreddsProps != null && ! userThreddsProps.isEmpty() )
        {
          if ( testdataDirPath == null )
            testdataDirPath = userThreddsProps.getProperty( testdataDirPropName );
        }
      }
    }

    // Use default paths if needed.
    if ( testdataDirPath == null )
    {
      System.out.println( "**No \"unidata.testdata.path\"property, defaulting to \"/share/testdata/\"." );
      testdataDirPath = "/share/testdata/";
    }
    // Make sure paths ends with a slash.
    if ((!testdataDirPath.endsWith( "/")) && !testdataDirPath.endsWith( "\\"))
      testdataDirPath += "/";

    testdataDir = testdataDirPath;
    cdmUnitTestDir = testdataDirPath + "cdmUnitTest/";

    File file = new File( cdmUnitTestDir );
    if ( ! file.exists() || !file.isDirectory() )
    {
      System.out.println( "**WARN: Non-existence of Level 3 test data directory [" + file.getAbsolutePath() + "]." );
    }

    File tmpDataDir = new File(temporaryLocalDataDir);
    if ( ! tmpDataDir.exists() )
    {
      if ( ! tmpDataDir.mkdirs() )
      {
        System.out.println( "**ERROR: Could not create temporary data dir <" + tmpDataDir.getAbsolutePath() + ">." );
      }
    }
  }


    ////////////////////////////////////////////////

  public interface Act {
    /**
     * @param filename file to act on
     * @return count
     * @throws IOException  on IO error
     */
    int doAct( String filename) throws IOException;
  }

  public static class FileFilterWant implements FileFilter {
    String[] suffixes;
    public FileFilterWant(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return true;
      return false;
    }
  }

  public static class FileFilterNoWant implements FileFilter {
    String[] suffixes;
    public FileFilterNoWant(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s: suffixes)
        if (file.getPath().endsWith(s)) return false;
      return true;
    }
  }

  public static int actOnAll(String dirName, FileFilter ff, Act act) throws IOException {
    return actOnAll( dirName, ff, act, true);
  }

  /**
   * @param dirName recurse into this directory
   * @param ff for files that pass this filter, may be null
   * @param act perform this acction
   * @return count
   * @throws IOException on IO error
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act, boolean recurse) throws IOException {
    int count = 0;

    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return count;
    }
    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude"))
        count += act.doAct(name);
    }

    if (!recurse) return count;

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude"))
        count += actOnAll(f.getAbsolutePath(), ff, act);
    }

    return count;
  }
}
