﻿/*
 * (The MIT license)
 * 
 * Copyright (c) 2012 MIPT (mr.santak@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies 
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ru.mipt.edf;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class EDF
{
	private static EDFParser parser = null;

	public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException
	{
		parser = new EDFParser();
		File file;
		if (args.length == 0)
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JFileChooser fileChooser = new JFileChooser();
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				file = fileChooser.getSelectedFile();
			else
				return;
		} else
			file = new File(args[0]);

		new File(file.getParent() + "/data").getAbsoluteFile().mkdir();

		InputStream is = null;
		FileOutputStream fos = null;
		InputStream format = null;
		try
		{
			is = new FileInputStream(file);
			fos = new FileOutputStream(file.getParent() + "/" + file.getName().replaceAll("[.].*", "_header.txt"));
			format = EDFParser.class.getResourceAsStream("header.format");
			parser.parseEDF(is);
			writeHeaderData(fos, getPattern(format));
		} finally
		{
			close(is);
			close(fos);
			close(format);
		}
		String channelFormat = null;
		format = null;
		try
		{
			format = EDFParser.class.getResourceAsStream("channel_info.format");
			channelFormat = getPattern(format);
		} finally
		{
			close(format);
		}

		for (int i = 0; i < parser.getNumberOfChannels(); i++)
		{
			try
			{
				fos = new FileOutputStream(file.getParent() + "/"
						+ file.getName().replaceAll("[.].*", "_channel_info_" + i + ".txt"));
				writeChannelData(fos, channelFormat, i);
			} finally
			{
				close(fos);
			}

			try
			{
				fos = new FileOutputStream(file.getParent() + "/data/"
						+ file.getName().replaceAll("[.].*", "_" + i + ".txt"));
				for (int j = 0; j < parser.getValuesInUnits(i).length; j++)
					fos.write((parser.getValuesInUnits(i)[j] + "\n").getBytes("UTF-8"));
			} finally
			{
				close(fos);
			}
		}
		List<Annotation> annotations = parser.getAnnotations();
		if (annotations == null || annotations.size() == 0)
			return;
		try
		{
			fos = new FileOutputStream(file.getParent() + "/" + file.getName().replaceAll("[.].*", "_annotation.txt"));
			for (Annotation annotation : annotations)
			{
				if (annotation.getAnnotations().size() != 0)
				{
					StringBuffer buffer = new StringBuffer();
					buffer.append(annotation.getOnSet()).append(";").append(annotation.getDuration());
					for (int i = 0; i < annotation.getAnnotations().size(); i++)
						buffer.append(";").append(annotation.getAnnotations().get(i));
					buffer.append("\n");
					fos.write(buffer.toString().getBytes());
				}
			}
		} finally
		{
			close(fos);
		}
	}

	private static void writeHeaderData(OutputStream os, String pattern) throws IOException
	{
		String message = MessageFormat.format(pattern, parser.getIdCode().trim(), parser.getSubjectID().trim(), parser
				.getRecordingID().trim(), parser.getStartDate().trim(), parser.getStartTime().trim(), parser
				.getBytesInHeader(), parser.getFormatVersion().trim(), parser.getNumberOfRecords(), parser
				.getDurationOfRecords(), parser.getNumberOfChannels());
		os.write(message.getBytes("UTF-8"));
	}

	private static void writeChannelData(OutputStream os, String pattern, int i) throws IOException
	{
		String message = MessageFormat.format(pattern, parser.getChannelLabels(i).trim(), parser.getTransducerTypes(i)
				.trim(), parser.getDimensions(i).trim(), parser.getMinInUnits(i), parser.getMaxInUnits(i), parser
				.getDigitalMin(i), parser.getDigitalMax(i), parser.getPrefilterings(i).trim(), parser
				.getNumberOfSamples(i), new String(parser.getReserveds(i)).trim());
		os.write(message.getBytes("UTF-8"));
	}

	private static String getPattern(InputStream is)
	{
		StringBuilder str = new StringBuilder();
		Scanner scn = null;
		try
		{
			scn = new Scanner(is);
			while (scn.hasNextLine())
				str.append(scn.nextLine()).append("\n");
		} finally
		{
			close(scn);
		}
		return str.toString();
	}

	private static final void close(Closeable c)
	{
		try
		{
			c.close();
		} catch (Exception e)
		{
			// do nothing
		}
	}
}
