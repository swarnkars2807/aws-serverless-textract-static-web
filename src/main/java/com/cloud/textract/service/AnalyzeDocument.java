package com.cloud.textract.service;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.AnalyzeDocumentRequest;
import com.amazonaws.services.textract.model.AnalyzeDocumentResult;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.Document;
import com.amazonaws.services.textract.model.Relationship;
import com.amazonaws.services.textract.model.S3Object;
import com.cloud.textract.model.Data;

public class AnalyzeDocument {
	
	private final String BUCKET_NAME = System.getenv("BUCKET_NAME");
	private final String KEY_NAME = System.getenv("KEY_NAME");

	BufferedImage image;

	AnalyzeDocumentResult result;

	Map<String, Block> lineItem = new LinkedHashMap<String, Block>();
	Map<String, Block> keyItem = new LinkedHashMap<String, Block>();
	Map<String, Block> valueItem = new LinkedHashMap<String, Block>();

	Map<String, String> keyValue = new LinkedHashMap<String, String>();
	
	
	List<Data> values;

	String key = null;
	String value = "";

	public AnalyzeDocument(AnalyzeDocumentResult documentResult) throws Exception {
		super();
		result = documentResult; // Results of text detection.
	}

	public AnalyzeDocument() throws Exception {
		super();
	}

	private void prepareLineBlocks(Block block) {
		if (block.getBlockType().equals("LINE")) {
			lineItem.put(block.getId(), block);
		}
	}

	// Draws the image and text bounding box.
	public void processBlockResult() {

		List<Block> blocks = result.getBlocks();
		Collections.sort(blocks, new Comparator<Block>() {
            @Override
            public int compare(Block a1, Block a2) {
                if (a1.getGeometry().getBoundingBox().getTop() > a2.getGeometry().getBoundingBox().getTop()) {
                    return 1;
                }
                if (a1.getGeometry().getBoundingBox().getTop() < a2.getGeometry().getBoundingBox().getTop()) {
                    return -1;
                }
                return 0;
            }
        });
		
		for (Block block : blocks) {
			prepareLineBlocks(block);
			switch (block.getBlockType()) {

			case "KEY_VALUE_SET":
				if (block.getEntityTypes().contains("KEY")) {
					keyItem.put(block.getId(), block);
				} else { // VALUE
					valueItem.put(block.getId(), block);
				}
				break;
			default:
			}
		}

		for (Block keyBlock : keyItem.values()) {

			List<Relationship> relationships = keyBlock.getRelationships();
			if (relationships != null) {
				for (Relationship r : relationships) {
					if (r.getType().equals("CHILD")) {
						for (Block lblock : lineItem.values()) {
							for (Relationship r2 : lblock.getRelationships()) {
								if (r2.getIds().contains(r.getIds().get(0))) {
									key = lblock.getText();
									if (keyValue.containsKey(null)) {
										keyValue.put(key, keyValue.get(null));
										keyValue.remove(null);
									} else {
										keyValue.put(key, "");
									}
								}
							}
						}
					} else if (r.getType().equals("VALUE")) {
						Block vBlock = valueItem.get(r.getIds().get(0));
						List<Relationship> relationships2 = vBlock.getRelationships();
						if (relationships2 != null) {
							for (Relationship r3 : vBlock.getRelationships()) {
								for (Block lblock : lineItem.values()) {
									for (Relationship r2 : lblock.getRelationships()) {
										if (r2.getIds().contains(r3.getIds().get(0))) {
											value = lblock.getText();
											if (keyValue.size() == 0) {
												keyValue.put(null, value);
											} else {
												keyValue.put(key, value);
											}
										}
									}
								}
							}
						}
					}
					key = null;
				}
			}

		}

		System.out.println(keyValue);
		
		keyValue.keySet().forEach(key -> {
			values.add(new Data(key, keyValue.get(key)));
		});
		

	}

	public List<Data> getOutput(String fileName) throws FileNotFoundException, IOException {

		System.out.println("S3 Bucket " + BUCKET_NAME + " keyName == " + KEY_NAME);
		
		values = new ArrayList<>();
		// Get image bytes
		/*
		 * ByteBuffer imageBytes = null; try (InputStream in = new
		 * FileInputStream("./documents/form.jpg")) { imageBytes =
		 * ByteBuffer.wrap(IOUtils.toByteArray(in)); }
		 */

		// Call AnalyzeDocument
		BasicAWSCredentials awsCreds = new BasicAWSCredentials("AKIA5V2I5CDEFJ33AO6O",
				"ca7EP7MzsB2uz13qNlX2XhJs9TslEZju2CLzEiOt");
		


		AmazonTextract client = AmazonTextractClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();

		AnalyzeDocumentRequest request = new AnalyzeDocumentRequest().withFeatureTypes("TABLES", "FORMS")
				.withDocument(new Document()
                        .withS3Object(new S3Object()
                                .withName(fileName)
                                .withBucket(BUCKET_NAME)));

		result = client.analyzeDocument(request);

		processBlockResult();

		return values;

	}

	/*
	 * public static void main(String arg[]) throws Exception {
	 * 
	 * // Get image bytes ByteBuffer imageBytes = null; try (InputStream in = new
	 * FileInputStream("./documents/form.jpg")) { imageBytes =
	 * ByteBuffer.wrap(IOUtils.toByteArray(in)); }
	 * 
	 * // Call AnalyzeDocument BasicAWSCredentials awsCreds = new
	 * BasicAWSCredentials("AKIA5V2I5CDEFJ33AO6O",
	 * "ca7EP7MzsB2uz13qNlX2XhJs9TslEZju2CLzEiOt");
	 * 
	 * AmazonTextract client = AmazonTextractClientBuilder.standard()
	 * .withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
	 * 
	 * AnalyzeDocumentRequest request = new
	 * AnalyzeDocumentRequest().withFeatureTypes("TABLES", "FORMS")
	 * .withDocument(new Document().withBytes(imageBytes));
	 * 
	 * result = client.analyzeDocument(request);
	 * 
	 * processBlockResult();
	 * 
	 * }
	 */
}