package com.example.demo.controller;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
	
	private final String TEMPLATE_DIR = "video/";
	private final String UPLOAD_DIR = "classpath:static/video/";

	private final ResourceLoader resourceLoader;

	@GetMapping("/region")
	public String videoRegion(Model model) {
		model.addAttribute("videoUrl", "/video/region/video.mp4");
		return TEMPLATE_DIR + "video";
	}

	@GetMapping("/region/{fileName}")
	public ResponseEntity<ResourceRegion> videoRegionFileName(
			@PathVariable String fileName,
			@RequestHeader HttpHeaders headers) throws IOException {

		Resource resource = resourceLoader.getResource(UPLOAD_DIR + fileName);

		final long chunkSize = 1024 * 1024 * 1;
		long contentLength = resource.contentLength();
		ResourceRegion region;
		try {
			HttpRange httpRange = headers.getRange().stream().findFirst().get();
			long start = httpRange.getRangeStart(contentLength);
			long end = httpRange.getRangeEnd(contentLength);
			long rangeLength = Long.min(chunkSize, end - start + 1);
			region = new ResourceRegion(resource, start, rangeLength);
		} catch (Exception e) {
			long rangeLength = Long.min(chunkSize, contentLength);
			region = new ResourceRegion(resource, 0, rangeLength);
		}
		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
				.contentType(MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM))
				.header("Accept-Ranges", "bytes").eTag(fileName).body(region);
	}

}
