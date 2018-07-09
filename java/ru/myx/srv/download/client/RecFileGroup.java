/*
 * Created on 24.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import ru.myx.ae3.help.Create;
import ru.myx.ae3.help.Text;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class RecFileGroup {
	private final int			fldLuid;
	
	private final String		name;
	
	private final Set<String>	types;
	
	private final RecFile		first;
	
	private final List<RecFile>	files;
	
	private final String		level2Name;
	
	private final String		level3Name;
	
	private List<RecFile>		filesHistory	= null;
	
	private List<RecFile>		filesLog		= null;
	
	RecFileGroup(final DownloadClient client,
			final String name,
			final String guid,
			final int itmLuid,
			final int fldLuid,
			final String fileName,
			final String md5,
			final long size,
			final long date,
			final String type,
			final String comment,
			final boolean preview,
			final String level2Name,
			final String level3Name,
			final byte[] description) {
		this.fldLuid = fldLuid;
		this.name = name;
		this.level2Name = level2Name;
		this.level3Name = level3Name;
		this.types = Create.tempSet();
		this.files = new ArrayList<>();
		this.first = new RecFile( client,
				guid,
				itmLuid,
				fldLuid,
				fileName,
				md5,
				size,
				date,
				type,
				comment,
				preview,
				level2Name,
				level3Name,
				description );
		this.addFile( this.first );
	}
	
	/**
	 * @param file
	 */
	public final void addFile(final RecFile file) {
		this.files.add( file );
		this.types.add( file.getType() );
	}
	
	@Override
	public boolean equals(final Object obj) {
		return obj == this || obj instanceof RecFileGroup && ((RecFileGroup) obj).getName().equals( this.getName() );
	}
	
	/**
	 * @return string
	 */
	public final String getComment() {
		return Text.join( this.types, ", " );
	}
	
	/**
	 * @return list
	 */
	public List<RecFile> getFiles() {
		return this.files;
	}
	
	/**
	 * @param sort
	 * @return list
	 */
	public List<RecFile> getFiles(final String sort) {
		if (sort == null || sort.length() == 0) {
			return this.files;
		}
		if ("history".equals( sort )) {
			if (this.filesHistory == null) {
				final RecFile[] filesHistory = this.files.toArray( new RecFile[this.files.size()] );
				Arrays.sort( filesHistory, ComparatorFileHistory.INSTANCE );
				this.filesHistory = Arrays.asList( filesHistory );
			}
			return this.filesHistory;
		}
		if ("log".equals( sort )) {
			if (this.filesLog == null) {
				final RecFile[] filesLog = this.files.toArray( new RecFile[this.files.size()] );
				Arrays.sort( filesLog, ComparatorFileLog.INSTANCE );
				this.filesLog = Arrays.asList( filesLog );
			}
			return this.filesLog;
		}
		return this.files;
	}
	
	/**
	 * @return file
	 */
	public final RecFile getFirst() {
		return this.first;
	}
	
	/**
	 * @return int
	 */
	public final int getFolderLuid() {
		return this.fldLuid;
	}
	
	/**
	 * @return string
	 */
	public final String getLevel2Name() {
		return this.level2Name;
	}
	
	/**
	 * @return string
	 */
	public final String getLevel3Name() {
		return this.level3Name;
	}
	
	/**
	 * @return string
	 */
	public final String getName() {
		return this.name;
	}
	
	/**
	 * @return int
	 */
	public int getSize() {
		return this.files.size();
	}
	
	/**
	 * @return set
	 */
	public final Set<String> getTypes() {
		return this.types;
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	
	@Override
	public String toString() {
		return "RecFileGroup{name:\"" + this.name + "\", size:" + this.files.size() + "}";
	}
}
