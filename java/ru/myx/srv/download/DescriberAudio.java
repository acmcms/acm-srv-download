/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.report.Report;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberAudio implements Describer {
	
	private static final Object DEFAULT_THUMBNAIL_BITRATE = System.getProperty("tritonus.lame.bitrate", "8");
	
	private static final boolean LINUX = System.getProperty("os.name", "").equalsIgnoreCase("Linux");
	
	private static final AudioFormat.Encoding OUT_ENCODING = new AudioFormat.Encoding("MPEG1L3");
	
	private static final AudioFileFormat.Type OUT_TYPE = new AudioFileFormat.Type("MP3", ".mp3");
	
	private static final float DELTA = 1E-9F;
	
	private static boolean equals(final float f1, final float f2) {
		
		return Math.abs(f1 - f2) < DescriberAudio.DELTA;
	}
	
	static boolean isPcm(final AudioFormat.Encoding encoding) {
		
		return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED);
	}
	
	private static final boolean stringValid(final String string) {
		
		for (int i = string.length() - 1; i >= 0; --i) {
			if (Character.isISOControl(string.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	private final Object thumbnailBitrate;
	
	private final AudioFormat outFormat;
	
	DescriberAudio(final BaseObject settings) {
		this.thumbnailBitrate = Base.getJava(settings, "thumbnailAudioBitrate", DescriberAudio.DEFAULT_THUMBNAIL_BITRATE);
		this.outFormat = new AudioFormat(DescriberAudio.OUT_ENCODING, -1, -1, 1, -1, -1, false, Collections.singletonMap("tritonus.lame.bitrate", this.thumbnailBitrate));
	}
	
	@Override
	public void buildTemporaryFiles(final File source, final TransferCollector preview1, final TransferCollector preview2) {
		
		try {
			if (DescriberAudio.LINUX) {
				final File tempOut;
				final File tempIn;
				{
					tempOut = File.createTempFile("prv", ".mp3", Engine.PATH_TEMP);
					tempIn = File.createTempFile("src", ".mp3", Engine.PATH_TEMP);
				}
				try {
					final String line = "lame -a --silent -h -b 8 " + tempIn.getAbsolutePath() + " " + tempOut.getAbsolutePath();
					Report.info("DLSRV", "Unix preview:\n" + source.getAbsolutePath() + "\n" + line);
					Transfer.toStream(Transfer.createBuffer(source), new FileOutputStream(tempIn), true);
					final Process process = Runtime.getRuntime().exec(line);
					final InputStream stderr = process.getErrorStream();
					final TransferCollector error = Transfer.createCollector();
					final OutputStream output = error.getOutputStream();
					final byte[] buffer = new byte[1024];
					for (;;) {
						final int read = stderr.read(buffer);
						if (read <= 0) {
							break;
						}
						output.write(buffer, 0, read);
					}
					process.waitFor();
					Thread.sleep(10000L);
					Transfer.toStream(stderr, error.getOutputStream(), true);
					final TransferBuffer errorBuffer = error.toBuffer();
					if (errorBuffer.hasRemaining()) {
						Report.warning("DLSRV", "While building unix preview: \nLine: " + line + "\n" + errorBuffer.toString(StandardCharsets.UTF_8));
					} else {
						Report.info("DLSRV", "Lame finished: " + source);
					}
					Transfer.toStream(Transfer.createBuffer(tempOut), preview1.getOutputStream(), false);
				} catch (final Throwable t) {
					Report.exception("DLSRV", "While building unix preview", t);
				} finally {
					tempOut.delete();
					tempIn.delete();
				}
			} else {
				AudioInputStream converted = AudioSystem.getAudioInputStream(source);
				final int frameBlock;
				final long frameLength;
				{
					final long originalFrameLength = converted.getFrameLength();
					final int originalFrameSize = converted.getFormat().getFrameSize();
					if (originalFrameLength != AudioSystem.NOT_SPECIFIED && originalFrameSize != AudioSystem.NOT_SPECIFIED) {
						frameLength = originalFrameLength;
						frameBlock = 1;
					} else {
						final AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(source);
						final Map<?, ?> properties = fileFormat.properties();
						final long duration = Convert.MapEntry.toLong(properties, "duration", 0L);
						if (duration > 0L) {
							final double inputDuration = duration / (1000.0d * 1000.0d);
							final double inputFrameCount = inputDuration * fileFormat.getFormat().getFrameRate();
							final double outputFrameCount = inputDuration * fileFormat.getFormat().getSampleRate();
							frameBlock = (int) Math.round(outputFrameCount / inputFrameCount);
							frameLength = (long) Math.floor(outputFrameCount - frameBlock);
						} else {
							frameLength = AudioSystem.NOT_SPECIFIED;
							frameBlock = -1;
						}
					}
				}
				if (!DescriberAudio.isPcm(converted.getFormat().getEncoding())) {
					final AudioFormat sourceFormat = converted.getFormat();
					final AudioFormat.Encoding targetEncoding = sourceFormat.getSampleSizeInBits() == 8
						? AudioFormat.Encoding.PCM_UNSIGNED
						: AudioFormat.Encoding.PCM_SIGNED;
					final AudioFormat pcm = new AudioFormat(
							targetEncoding,
							sourceFormat.getSampleRate(),
							16,
							sourceFormat.getChannels(),
							sourceFormat.getChannels() * 2, // frameSize
							sourceFormat.getSampleRate(),
							false);
					converted = AudioSystem.getAudioInputStream(pcm, converted);
				}
				if (converted.getFormat().getChannels() != 1) {
					final AudioFormat sourceFormat = converted.getFormat();
					if (sourceFormat.getSampleSizeInBits() == 8) {
						final AudioFormat targetFormat = new AudioFormat(
								AudioFormat.Encoding.PCM_UNSIGNED,
								sourceFormat.getSampleRate(),
								8,
								1,
								1,
								sourceFormat.getFrameRate(),
								sourceFormat.isBigEndian());
						converted = new AudioInputStream(new InputStreamStereo8ToMono8(converted, frameBlock, frameLength), targetFormat, frameLength);
					} else {
						final AudioFormat targetFormat = new AudioFormat(
								AudioFormat.Encoding.PCM_SIGNED,
								sourceFormat.getSampleRate(),
								16,
								1,
								2,
								sourceFormat.getFrameRate(),
								sourceFormat.isBigEndian());
						converted = new AudioInputStream(new InputStreamStereo16ToMono16(converted, frameBlock, frameLength), targetFormat, frameLength);
					}
				}
				if (converted.getFormat().getSampleSizeInBits() == 8) {
					final AudioFormat sourceFormat = converted.getFormat();
					final AudioFormat targetFormat = new AudioFormat(
							AudioFormat.Encoding.PCM_SIGNED,
							sourceFormat.getSampleRate(),
							16,
							1,
							2,
							sourceFormat.getFrameRate(),
							sourceFormat.isBigEndian());
					converted = new AudioInputStream(new InputStreamMono8ToMono16(converted, frameBlock, frameLength), targetFormat, frameLength);
				}
				if (false && converted.getFormat().getChannels() != 1) {
					final AudioFormat sourceFormat = converted.getFormat();
					final AudioFormat.Encoding targetEncoding = sourceFormat.getSampleSizeInBits() == 8
						? AudioFormat.Encoding.PCM_UNSIGNED
						: AudioFormat.Encoding.PCM_SIGNED;
					final AudioFormat targetFormat = new AudioFormat(
							targetEncoding,
							sourceFormat.getSampleRate(),
							8,
							1,
							(8 + 7) / 8,
							sourceFormat.getFrameRate(),
							sourceFormat.isBigEndian());
					converted = sourceFormat.getSampleSizeInBits() == 16
						? new AudioInputStream(sourceFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
							? (InputStream) new InputStreamSignedStereoMono16(converted)
							: (InputStream) new InputStreamUnsignedStereoMono16(converted), targetFormat, frameLength)
						: new AudioInputStream(new InputStreamStereoMono8(converted), targetFormat, frameLength);
				} else //
				if (false && converted.getFormat().getSampleSizeInBits() != 8) {
					final AudioFormat sourceFormat = converted.getFormat();
					final AudioFormat targetFormat = new AudioFormat(
							AudioFormat.Encoding.PCM_UNSIGNED,
							sourceFormat.getSampleRate(),
							8,
							1,
							(8 + 7) / 8,
							sourceFormat.getFrameRate(),
							sourceFormat.isBigEndian());
					converted = new AudioInputStream(sourceFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
						? (InputStream) new InputStreamSignedMono16(converted)
						: (InputStream) new InputStreamUnsignedMono16(converted), targetFormat, frameLength);
				}
				if (false && !DescriberAudio.equals(converted.getFormat().getSampleRate(), 8000.0f)) {
					final AudioFormat sourceFormat = converted.getFormat();
					final AudioFormat targetFormat = new AudioFormat(
							sourceFormat.getEncoding(),
							8000.0f,
							sourceFormat.getSampleSizeInBits(),
							sourceFormat.getChannels(),
							sourceFormat.getFrameSize(),
							8000.0f,
							sourceFormat.isBigEndian());
					converted = AudioSystem.getAudioInputStream(targetFormat, converted);
				}
				Report.info("DLSRV", "converting: name=" + source.getName() + ", format=" + converted.getFormat());
				converted = AudioSystem.getAudioInputStream(this.outFormat, converted);
				AudioSystem.write(converted, DescriberAudio.OUT_TYPE, preview1.getOutputStream());
			}
		} catch (final Exception e) {
			Report.exception("DLSRV", "While building preview", e);
		}
	}
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final AudioFileFormat format;
		{
			AudioFileFormat formatCheck;
			try {
				formatCheck = AudioSystem.getAudioFileFormat(file);
			} catch (final Throwable t) {
				try (final InputStream input = new BufferedInputStream(new FileInputStream(file))) {
					formatCheck = AudioSystem.getAudioFileFormat(input);
				} catch (final Throwable t2) {
					final StringBuilder result = new StringBuilder();
					result.append("audio, unknown");
					final long size = file.length();
					if (size > 0) {
						result.append(", ").append(Format.Compact.toBytes(size)).append('B');
					}
					target.baseDefine("output", result.toString());
					return true;
				}
			}
			format = formatCheck;
		}
		final AudioFormat formatAudio = format.getFormat();
		final Map<?, ?> properties = format.properties();
		final float sampleRate = formatAudio.getSampleRate();
		final int bits = formatAudio.getSampleSizeInBits();
		final int channels = formatAudio.getChannels();
		final StringBuilder result = new StringBuilder();
		result.append("audio, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		final long duration = Convert.MapEntry.toLong(properties, "duration", 0L);
		if (duration > 0L) {
			result.append(", ").append(duration / (1000L * 1000L * 60L)).append(':');
			final int seconds = (int) (duration / (1000L * 1000L) % 60L);
			if (seconds < 10) {
				result.append('0');
			}
			result.append(seconds);
			if (size > 0) {
				result.append(", ").append(Math.round(8.0d * size / (duration / (1000.0d * 1000.0d)) / 1000.0d)).append(" kbps");
			}
		} else {
			final long duration2 = (long) (1000L * 1000L * format.getFrameLength() / sampleRate);
			if (duration2 > 0L) {
				result.append(", ").append(duration2 / (1000L * 1000L * 60L)).append(':');
				final int seconds = (int) (duration2 / (1000L * 1000L) % 60L);
				if (seconds < 10) {
					result.append('0');
				}
				result.append(seconds);
				if (size > 0) {
					result.append(", ").append(Math.round(8.0d * size / (duration2 / (1000.0d * 1000.0d)) / 1000.0d)).append(" kbps");
				}
			}
		}
		if (sampleRate > 0) {
			result.append(", ").append(Format.Compact.toDecimal(sampleRate)).append("Hz");
		}
		if (bits > 0) {
			result.append(", ").append(bits).append("bit");
		}
		if (channels > 0) {
			result.append(", ");
			if (channels == 2) {
				result.append("stereo");
			} else //
			if (channels == 1) {
				result.append("mono");
			} else {
				result.append(channels).append("channels");
			}
		}
		final String trackTitle = Convert.MapEntry.toString(properties, "title", "").trim();
		if (trackTitle.length() > 0 && DescriberAudio.stringValid(trackTitle)) {
			target.baseDefine("title", trackTitle);
		}
		final String trackAlbum = Convert.MapEntry.toString(properties, "album", "").trim();
		if (trackAlbum.length() > 0 && DescriberAudio.stringValid(trackAlbum)) {
			target.baseDefine("album", trackAlbum);
		}
		final String trackAuthor = Convert.MapEntry.toString(properties, "author", "").trim();
		if (trackAuthor.length() > 0 && DescriberAudio.stringValid(trackAuthor)) {
			target.baseDefine("author", trackAuthor);
		}
		final int trackYear = Convert.MapEntry.toInt(properties, "year", -1);
		if (trackYear != -1) {
			target.baseDefine("year", trackYear);
		}
		target.baseDefine("output", result.toString());
		return true;
	}
	
	@Override
	public String getMediaType() {
		
		return "audio";
	}
	
	@Override
	public String getMediaTypeFor(final File file) {
		
		return FileName.extension(file) + " audio";
	}
	
	@Override
	public int getVersion() {
		
		return 3;
	}
	
	@Override
	public boolean isPreviewAvailable() {
		
		return false;
	}
	
	@Override
	public boolean isThumbnailAvailable() {
		
		return true;
	}
}
