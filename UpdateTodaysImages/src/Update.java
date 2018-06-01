import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;


public class Update {
	
	private static String path = "\\\\t65-w7-eqcash\\incoming\\ChalkServer\\Chalkimages\\";
	
	private static String[] getFileNames(String imageName, String today)
	{
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[]{imageName + today + "*.png"});
        scanner.setBasedir(path+"archive");
        scanner.scan();
        String[] files = scanner.getIncludedFiles();		
		return files;
	}
	
	private static void deleteFiles(String[] imageNames)
	{
        for (String name: imageNames)
        {
        	File image = new File(path+"archive\\"+name);
        	image.delete();
        }
	}
	
	private static void moveImages(String name, String[] imageNames) // name = Bell, Marb, Revisions
	{

		File s = new File(path+name+".png");
		
		for (String image: imageNames)
		{
			File d = new File(path+"archive\\"+image);
			try {
				FileUtils.copyFile(s, d);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
	}	
	
	
	public static void main(String[] args) {
		
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(cal.getTime());
        
        String[] revisionsImageNames = getFileNames("Revisions", today);
        deleteFiles(revisionsImageNames);
        moveImages("Revisions",revisionsImageNames);
        
        String[] bellImageNames = getFileNames("Bell", today);
        deleteFiles(bellImageNames);
        moveImages("Bell",bellImageNames);
        
        String[] marbImageNames = getFileNames("Marb", today);
        deleteFiles(marbImageNames);
        moveImages("Marb",marbImageNames);

		// Git commit/push images to remote repository
		Process p;
		try {
			p = Runtime.getRuntime().exec("wmic /user:equitydma /password:123456Ad /node:\"t65-w7-eqcash\" process call create \"cmd /c C:\\incoming\\ChalkServer\\gitpush.bat\"");
			p.waitFor();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Exception ocurred: git push to images remote repository");
		}        
        
        
	}
	

}
