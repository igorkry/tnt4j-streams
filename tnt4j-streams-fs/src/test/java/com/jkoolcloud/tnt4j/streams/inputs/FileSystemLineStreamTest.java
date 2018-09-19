package com.jkoolcloud.tnt4j.streams.inputs;

import java.nio.file.spi.FileSystemProvider;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class FileSystemLineStreamTest {

	@Test
	public void checkSupportedFileSystems() {
		List<FileSystemProvider> fileSystemProviders = FileSystemProvider.installedProviders();
		System.out.println(fileSystemProviders);
		Iterator<FileSystemProvider> iterator = fileSystemProviders.iterator();
		while (iterator.hasNext()) {
			System.out.println(iterator.next().getScheme());
			System.out.println("\n");
		}

	}

}