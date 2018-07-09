/*
 * Created on 31.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.Transfer;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCollector;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.help.FileName;
import ru.myx.ae3.help.Format;
import ru.myx.ae3.mime.MimeType;

/**
 * @author myx
 * 		
 *         Window - Preferences - Java - Code Style - Code Templates
 */
class DescriberImage implements Describer {
	
	static final int DEFAULT_THUMBNAIL_WIDTH = Convert.MapEntry.toInt(System.getProperties(), "thumbnail_image_width", 120);
	
	static final int DEFAULT_THUMBNAIL_HEIGHT = Convert.MapEntry.toInt(System.getProperties(), "thumbnail_image_height", 120);
	
	static final String DEFAULT_THUMBNAIL_FORMAT = Convert.MapEntry.toString(System.getProperties(), "thumbnail_image_format", "jpeg");
	
	static final int DEFAULT_PREVIEW_WIDTH = Convert.MapEntry.toInt(System.getProperties(), "preview_image_width", 400);
	
	static final int DEFAULT_PREVIEW_HEIGHT = Convert.MapEntry.toInt(System.getProperties(), "preview_image_width", -1);
	
	static final String DEFAULT_PREVIEW_FORMAT = Convert.MapEntry.toString(System.getProperties(), "preview_image_format", "jpeg");
	
	static final double DEFAULT_THUMBNAIL_HORIZONTAL = Convert.MapEntry.toDouble(System.getProperties(), "thumbnail_image_horizontal", 1.25);
	
	static final double DEFAULT_THUMBNAIL_VERTICAL = Convert.MapEntry.toDouble(System.getProperties(), "thumbnail_image_vertical", 1.25);
	
	static final double DEFAULT_PREVIEW_HORIZONTAL = Convert.MapEntry.toDouble(System.getProperties(), "preview_image_horizontal", 1.0);
	
	static final double DEFAULT_PREVIEW_VERTICAL = Convert.MapEntry.toDouble(System.getProperties(), "preview_image_vertical", 1.0);
	
	private static final void buildPreviewFile(final File source,
			final BufferedImage image,
			final String format,
			final int widthLimit,
			final int heightLimit,
			final double horizontal,
			final double vertical,
			final TransferCollector target) throws Exception {
			
		if (image == null) {
			return;
		}
		final BufferedImage result;
		final int compareWidth = widthLimit > 0
			? widthLimit
			: Integer.MAX_VALUE;
		final int compareHeight = heightLimit > 0
			? heightLimit
			: Integer.MAX_VALUE;
		final Dimension dimension = new Dimension(image.getWidth(), image.getHeight());
		if (dimension.width < compareWidth && dimension.height < compareHeight) {
			result = image;
		} else {
			final double kW = dimension.width < compareWidth
				? 1.0
				: 1.0 * dimension.width / compareWidth;
			final double kH = dimension.height < compareHeight
				? 1.0
				: 1.0 * dimension.height / compareHeight;
			if (kW > kH) {
				final int width = widthLimit;
				final int widthMargin = (int) Math.round(width - width / horizontal);
				final int rw = image.getWidth();
				final int rh = image.getHeight();
				final float k = 1.0f * width / rw;
				final int height = (int) Math.ceil(rh * k);
				final int heightMargin = (int) Math.ceil(widthMargin * rh / rw);
				result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				final Graphics2D graphics2D = result.createGraphics();
				graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics2D.drawImage(image, 0 - widthMargin, 0 - heightMargin, width + widthMargin * 2, height + heightMargin * 2, null);
			} else {
				final int height = heightLimit;
				final int heightMargin = (int) Math.round(height - height / vertical);
				final int rw = image.getWidth();
				final int rh = image.getHeight();
				final float k = 1.0f * height / rh;
				final int width = (int) Math.ceil(rw * k);
				final int widthMargin = (int) Math.ceil(heightMargin * rw / rh);
				result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				final Graphics2D graphics2D = result.createGraphics();
				graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics2D.drawImage(image, 0 - widthMargin, 0 - heightMargin, width + widthMargin * 2, height + heightMargin * 2, null);
			}
		}
		{
			@SuppressWarnings("resource")
			final TransferCollector temp = Transfer.createCollector();
			ImageIO.write(result, format, temp.getOutputStream());
			temp.close();
			final TransferBuffer resultBuffer = temp.toBuffer();
			if (resultBuffer.remaining() < source.length()) {
				Transfer.toStream(resultBuffer, target.getOutputStream(), false);
			} else {
				Transfer.toStream(Transfer.createBuffer(source), target.getOutputStream(), false);
			}
		}
	}
	
	private final int thumbnailWidth;
	
	private final int thumbnailHeight;
	
	private final String thumbnailFormat;
	
	private final double thumbnailHorizontal;
	
	private final double thumbnailVertical;
	
	private final int previewWidth;
	
	private final int previewHeight;
	
	private final String previewFormat;
	
	private final double previewHorizontal;
	
	private final double previewVertical;
	
	DescriberImage(final BaseObject settings) {
		this.thumbnailWidth = Convert.MapEntry.toInt(settings, "thumbnailImageWidth", DescriberImage.DEFAULT_THUMBNAIL_WIDTH);
		this.thumbnailHeight = Convert.MapEntry.toInt(settings, "thumbnailImageHeight", DescriberImage.DEFAULT_THUMBNAIL_HEIGHT);
		this.thumbnailFormat = Base.getString(settings, "thumbnailImageFormat", DescriberImage.DEFAULT_THUMBNAIL_FORMAT);
		this.previewWidth = Convert.MapEntry.toInt(settings, "previewImageWidth", DescriberImage.DEFAULT_PREVIEW_WIDTH);
		this.previewHeight = Convert.MapEntry.toInt(settings, "previewImageHeight", DescriberImage.DEFAULT_PREVIEW_HEIGHT);
		this.previewFormat = Base.getString(settings, "previewImageFormat", DescriberImage.DEFAULT_PREVIEW_FORMAT);
		this.thumbnailHorizontal = Convert.MapEntry.toDouble(settings, "thumbnailImageHorizontal", DescriberImage.DEFAULT_THUMBNAIL_HORIZONTAL);
		this.thumbnailVertical = Convert.MapEntry.toDouble(settings, "thumbnailImageVertical", DescriberImage.DEFAULT_THUMBNAIL_VERTICAL);
		this.previewHorizontal = Convert.MapEntry.toDouble(settings, "previewImageHorizontal", DescriberImage.DEFAULT_PREVIEW_HORIZONTAL);
		this.previewVertical = Convert.MapEntry.toDouble(settings, "previewImageVertical", DescriberImage.DEFAULT_PREVIEW_VERTICAL);
	}
	
	@Override
	public void buildTemporaryFiles(final File source, final TransferCollector preview1, final TransferCollector preview2) throws Exception {
		
		final BufferedImage image = ImageIO.read(source);
		if (image == null) {
			return;
		}
		if (preview1 != null) {
			DescriberImage
					.buildPreviewFile(source, image, this.thumbnailFormat, this.thumbnailWidth, this.thumbnailHeight, this.thumbnailHorizontal, this.thumbnailVertical, preview1);
		}
		if (preview2 != null) {
			DescriberImage.buildPreviewFile(source, image, this.previewFormat, this.previewWidth, this.previewHeight, this.previewHorizontal, this.previewVertical, preview2);
		}
	}
	
	@Override
	public boolean describe(final String type, final File file, final BaseObject target) throws Exception {
		
		final StringBuilder result = new StringBuilder();
		result.append("image, ");
		result.append(type);
		final long size = file.length();
		if (size > 0) {
			result.append(", ").append(Format.Compact.toBytes(size)).append('B');
		}
		if (file.exists()) {
			result.append(", ").append(MimeType.forFile(file, type));
		} else {
			throw new IOException("Not exists!");
		}
		target.baseDefine("output", result.toString());
		return true;
	}
	
	@Override
	public String getMediaType() {
		
		return "image";
	}
	
	@Override
	public String getMediaTypeFor(final File file) {
		
		return FileName.extension(file) + " image";
	}
	
	@Override
	public int getVersion() {
		
		return 4;
	}
	
	@Override
	public boolean isPreviewAvailable() {
		
		return true;
	}
	
	@Override
	public boolean isThumbnailAvailable() {
		
		return true;
	}
}
