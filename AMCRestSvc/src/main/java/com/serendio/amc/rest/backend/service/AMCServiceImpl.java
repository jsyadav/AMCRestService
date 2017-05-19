package com.serendio.amc.rest.backend.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;



@Service
public class AMCServiceImpl implements AmcService{
	private String backend_home = "/home/akksa/git/amc-backend/";

	@Override
	public int processJobId(String jobId) {
		// TODO Auto-generated method stub

		String cmd = "python " + backend_home  + "UpdateStartTimeForJob.py "+ jobId;
		callPython(cmd);

		cmd = "python " + backend_home  + "UpdateJobStatus.py "+ jobId + " AKKSAstep1";
		callPython(cmd);

		// Add code to FTP the file from PHP server to local server
		// TODO:......

		cmd = "python  " + backend_home  + "run_ocr.py " + jobId;
		String returnText = callPythonReturnString(cmd);
		String tempDir =  backend_home + "tmp/" + jobId;		
		File tempDirFileObject = new File(tempDir);
		if (!tempDirFileObject.exists()){
			cmd = "mkdir -p " + tempDir;
			callShellScript(cmd);	
		}
		else{
			System.out.println("The Director "+tempDir + " already exists");
		}




		cmd = "python  " + backend_home  +  "get_filepath.py " + jobId;
		String filePath = callPythonReturnString(cmd);
		File filePathObject = new File(filePath);
		String fileName = filePathObject.getName();
		cmd = "python  " + backend_home  +  "get_caseno.py " + jobId;
		String caseNo = callPythonReturnString(cmd);
		System.out.println("Case Number is " + caseNo);
		cmd = "cp "+filePath + " " + tempDir;
		callShellScript(cmd);
		String procFile =  tempDir + "/" + fileName;

		// Need class name to call
		cmd = "java -jar " + backend_home + "ioana/ioana-jar-with-dependencies.jar -i " +  procFile + " -o " +  tempDir + " -runMode 1";
		callShellScript(cmd);

		cmd = "python " + backend_home  +  "remove_non_printable_char.py "+ procFile + "_trimmed.txt " + procFile +"_trimmed.txt";
		callPython(cmd);

		cmd = "python " + backend_home  +  "UpdateJobStatus.py "+ jobId + " step2";
		callPython(cmd);	

		cmd = "python  " + backend_home  +  "runNLP.py " + procFile + "_trimmed.txt ";
		String response = callPythonReturnString(cmd);

		cmd = "python " + backend_home  +  "UpdateJobStatus.py "+ jobId + " step3";
		callPython(cmd);

		//cmd = "python getAttorneys.py "+ procFile;
		//callPython(cmd);

		String opFile = procFile + "_trimmed.txt.xml";

		// coreference-filter.sh
		List<String> perlCommand = new ArrayList<String>();

		cmd = "/usr/bin/perl " + backend_home + "pruneCoref.pl " + opFile +" > "+ opFile+".new";
		String newOutputFile = opFile+".new";
		perlCommand.add("/usr/bin/perl");
		perlCommand.add(backend_home + "pruneCoref.pl");
		perlCommand.add(opFile);
		callPerl(perlCommand, newOutputFile);
		perlCommand.clear();

		String colOutputFile = opFile+".col";
		perlCommand.add("/usr/bin/perl");
		perlCommand.add(backend_home + "xm2col1.pl");
		perlCommand.add(opFile);
		perlCommand.add("2");
		perlCommand.add(">");
		perlCommand.add(opFile+".parse");

		callPerl(perlCommand, colOutputFile);
		perlCommand.clear();


		String corefOutputFile = opFile+"_coref.out";
		perlCommand.add("/usr/bin/perl");
		perlCommand.add(backend_home + "getCorefChains_v1.pl");

		perlCommand.add(newOutputFile);
		perlCommand.add(colOutputFile);
		callPerl(perlCommand, corefOutputFile);

		/*cmd = "/usr/bin/perl " + backend_home + "xm2col1.pl " + opFile +" > "+ opFile+".col 2>" + opFile+".parse";
		callPerl(cmd);
		cmd = "/usr/bin/perl " + backend_home + "getCorefChains_v1.pl " + opFile +".new"+ opFile+".col >" + opFile +"_coref.out";

		// event_extraction.sh
		cmd = "perl cullout_vps_new_v5.pl " + opFile +" > "+ opFile+".inter_eveout";
		callPerl(cmd);
		cmd = "perl date.pl < " + opFile +"_coref.out > "+ opFile+"_coref.out.dateout";
		callPerl(cmd);
		cmd = "perl concatenateFile.pl " + opFile +"_coref.out.dateout"+ opFile+".inter_eveout >" + opFile +"_event.out";

		cmd = "python getSentencesDump.py "+ opFile+"_coref.out";
		callPython(cmd);

		// Call class name
		//java -jar lib/amc-jar-with-dependencies.jar ${op}.dmp ${op}_event.out.dmp

		//sh verticalRelationsExtract.sh ${op}_event_new.out $op > ${op}_vertical_rel.out
		cmd = "perl getRelations.pl " + opFile+"_event_new.out " ;
		callPerl(cmd);
		cmd = "perl getVRelations.pl " + opFile + " > "+ opFile+"_vertical_rel.out";
		callPerl(cmd);

		cmd = "python UpdateJobStatus.py "+ jobId + " step4";
		callPython(cmd);

		cmd = "python processCoref.py "+ opFile+"_coref.out "+ opFile+ "_event.out "+ opFile + " "+opFile+"_vertical_rel.out " + jobId;
		callPython(cmd);

		cmd = "python UpdateCompletedTimeForJob.py "+ jobId;
		callPython(cmd);

		cmd = "python taxonomy.py " + jobId;
		callPython(cmd);

		cmd = "python UpdateJobStatus.py "+ jobId +" completedAKKSA";
		callPython(cmd); 

		// call the solr import using java api
		//wget "http://fa3:8983/solr/amc_prod/dataimport?command=full-import"
		//rm "dataimport?command=full-import"


		// Add code to notify the user via an email
		// Integrate java mail
		// TODO:......

		 */
		return 0;

	}

	public static void callPython(String cmd) {
		long start = System.currentTimeMillis();
		try{

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//int ret = new Integer(in.readLine()).intValue();
			System.out.println("value is : "+in.readLine());
		}catch(Exception e){
			e.printStackTrace();
		}
		System.out.println("Time taken by "+ cmd + " " + (System.currentTimeMillis() - start));

	}
	public static String callPythonReturnString(String cmd) {
		String ret = null;
		long start = System.currentTimeMillis();
		try{

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			ret = in.readLine();
			System.out.println("value is : "+ret);
		}catch(Exception e){

		}
		System.out.println("Time taken by "+ cmd + " " + (System.currentTimeMillis() - start));
		return ret;

	}
	public static int callPythonReturnInt(String cmd) {
		int ret = 0;
		long start = System.currentTimeMillis();
		try{

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			ret = new Integer(in.readLine()).intValue();
			System.out.println("value is : "+ret);
		}catch(Exception e){

		}
		System.out.println("Time taken by "+ cmd + " " + (System.currentTimeMillis() - start));
		return ret;

	}
	public static void callShellScript(String cmd){
		long start = System.currentTimeMillis();
		try{

			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			System.out.println(in.readLine());
			//System.out.println("value is : "+ret);
		}catch(Exception e){
			System.out.println(e);
		}
		System.out.println("Time taken by "+ cmd + " " +(System.currentTimeMillis() - start));

	}
	public static void callPerl(List<String> perlCommand, String outputFile) {
		long start = System.currentTimeMillis();
		String inputFilename = "";
		try{
			inputFilename = perlCommand.get(1);
			ProcessBuilder processBuilder = new ProcessBuilder(perlCommand);

			//processBuilder.redirectOutput(new File(outputFile));
			Process p = processBuilder.start();
			/*Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//int ret = new Integer(in.readLine()).intValue();
			System.out.println("value is : "+in.readLine());*/
		}catch(Exception e){
			System.out.println(e);
		}
		System.out.println("Time taken by for "+ inputFilename + " " +(System.currentTimeMillis() - start));
	}

	public static void coreferenceFilter() {
		//sh coreference-filter.sh $op > ${op}_coref.out
		String in = "/Users/jyadav/JITENDRA/workspace/amc-rest-service/AMCRestSvc/files/News_3039.txt_trimmed.txt.xml";
		String inNew = in+".new";
		String inCol = in+".col";
		String inParse = in+".parse";
		String inCorefOut = in+"_coref.out";
		
		try {
			// perl prunCoref.pl $1 > $1.new
			String perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/pruneCoref.pl";
			Process p = new ProcessBuilder("/usr/bin/perl", perlFile, in).redirectErrorStream(true).start();
			String s;
			BufferedReader stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(inNew)));
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
			
			// perl xml2col1.pl $1 >$1.col 2>$1.parse
			perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/xml2col1.pl";
			p = new ProcessBuilder("/usr/bin/perl", perlFile, in).redirectErrorStream(false).start();
			stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			bw = new BufferedWriter(new FileWriter(new File(inCol)));
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
			stdout = new BufferedReader (
					new InputStreamReader(p.getErrorStream()));
			bw = new BufferedWriter(new FileWriter(new File(inParse)));
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
			
			// perl corefchains_v1.pl $1.new $1.col > $1_coref.out	
			perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/getCorefChains_v1.pl";
			p = new ProcessBuilder("/usr/bin/perl", perlFile, inNew, inCol).redirectErrorStream(true).start();
			stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			bw = new BufferedWriter(new FileWriter(new File(inCorefOut)));
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void eventExtraction() {
		//sh event_extraction.sh $op ${op}_coref.out > ${op}_event.out
		String in = "/Users/jyadav/JITENDRA/workspace/amc-rest-service/AMCRestSvc/files/News_3039.txt_trimmed.txt.xml";
		String inCorefOut = in+"_coref.out";
		String inInterEveout = in+".inter_eveout";
		String inCorefOutDateout = inCorefOut+"dateout";
		String inEventOut = in+"_event.out";
		
		try {
			// perl cullout_vps_new_v5.pl $1 >$1.inter_eveout
			String perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/cullout_vps_new_v5.pl";
			Process p = new ProcessBuilder("/usr/bin/perl", perlFile, in).redirectErrorStream(true).start();
			BufferedReader stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(inInterEveout)));
			String s;
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();

			// perl date.pl < $2 >$2.dateout
			perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/date.pl";
			boolean redirect = true;
			if (!redirect) {
				p = new ProcessBuilder("/usr/bin/perl", perlFile, inCorefOut).redirectErrorStream(true).start();
			}else {
				BufferedReader br = new BufferedReader(new FileReader(new File(inCorefOut)));
				p = new ProcessBuilder("/usr/bin/perl", perlFile).redirectErrorStream(true).start();
				BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
				while((s = br.readLine()) != null) {
					stdin.write(s+"\n");
				}
				br.close();
			}
			stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			bw = new BufferedWriter(new FileWriter(new File(inCorefOutDateout)));			
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
			
			// perl concatenateFile.pl $2.dateout $1.inter_eveout
			perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/concatenateFile.pl";
			p = new ProcessBuilder("/usr/bin/perl", perlFile, inCorefOutDateout, inInterEveout).redirectErrorStream(true).start();
			stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			bw = new BufferedWriter(new FileWriter(new File(inEventOut)));			
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void verticalRelationsExtracts() {
		// sh vertialRelationsExtract.sh ${op}_event_new.out $op > ${op}_vertical_rel.out
		String in = "/Users/jyadav/JITENDRA/workspace/amc-rest-service/AMCRestSvc/files/News_3039.txt_trimmed.txt.xml";
		String inEventnewOut = in+"_event_new.out";
		String inVerticalRelOut = in+"_vertical_rel.out";
		
		
		try {
			// perl getRelations.pl $1
			String perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/getRelations.pl";
			Process p = new ProcessBuilder("/usr/bin/perl", perlFile, inEventnewOut).redirectErrorStream(true).start();

			// perl getVRelations.pl $2
			perlFile = "/Users/jyadav/JITENDRA/AssistMyCase/getVRelations.pl";
			p = new ProcessBuilder("/usr/bin/perl", perlFile, in).redirectErrorStream(true).start();
			BufferedReader stdout = new BufferedReader (
					new InputStreamReader(p.getInputStream()));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(inVerticalRelOut)));
			String s;
			while ((s = stdout.readLine()) != null) {
				//System.out.println(s);
				bw.write(s+"\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			


	}
	
	
	public static void main(String[] args) {
		//coreferenceFilter();
		//eventExtraction();
		//verticalRelationsExtracts();
	}
}
