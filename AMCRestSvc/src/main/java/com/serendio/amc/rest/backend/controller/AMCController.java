package com.serendio.amc.rest.backend.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.serendio.amc.rest.backend.service.AmcService;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

@RestController
public class AMCController {
	private java.util.Properties stanfordProperty;
	private StanfordCoreNLP pipeline;
	private Annotation annotation;

	public AMCController() {
		super();
		stanfordProperty = new Properties();
		stanfordProperty.put("annotators",
				"tokenize,ssplit,pos,lemma,ner,parse,dcoref,sentiment");
		pipeline = new StanfordCoreNLP(stanfordProperty);

	}

	// Annotation annotation;

	@Autowired
	private AmcService amcService = null;

	@RequestMapping("/amc/upload")
	public int getSchedule(
			@RequestParam(value = "jobId", required = true) String jobId) {
		//return Integer.parseInt("100");
		 return amcService.processJobId(jobId);
	}

	@RequestMapping(value="/amc/processText",method = RequestMethod.POST)
	public String getSchedule2(
			@RequestParam(value = "text", required = true) String text) {
		Writer stanfordXMLWriter = new StringWriter();
		annotation = new Annotation(text);
		// return amcService.processJobId(jobId);
		pipeline.annotate(annotation);
		try {
			pipeline.xmlPrint(annotation, stanfordXMLWriter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stanfordXMLWriter.toString();
	}

}
