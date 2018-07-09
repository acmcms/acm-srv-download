/*
 * Created on 07.11.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client;

import java.util.Arrays;
import java.util.Set;

import ru.myx.ae3.help.Create;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
abstract class NameParser {
	private static final class Impl extends NameParser {
		private static final String getName(final String name) {
			final String result = name.trim();
			if (result.length() == 0) {
				return "*";
			}
			return result.replace( '`', '\'' ).replace( "  ", " " ).replace( " ft ", " feat. " )
					.replace( " ft. ", " feat. " ).replace( " feat ", " feat. " ).replace( " vs ", " vs. " )
					.replace( " featuring ", " feat. " );
		}
		
		private final String	level1;
		
		private final String	level2;
		
		private final String	level3;
		
		private final String	hint;
		
		Impl(final String level1, final String level2, final String level3) {
			this.level1 = Impl.getName( level1 );
			this.level2 = Impl.getName( level2 );
			this.level3 = Impl.getName( level3 );
			this.hint = "*";
		}
		
		Impl(final String level1, final String level2, final String level3, final String hint) {
			this.level1 = Impl.getName( level1 );
			this.level2 = Impl.getName( level2 );
			this.level3 = Impl.getName( level3 );
			this.hint = hint;
		}
		
		@Override
		public String getHint() {
			return this.hint;
		}
		
		@Override
		public String getLevel1Name() {
			return this.level1;
		}
		
		@Override
		public String getLevel2Name() {
			return this.level2;
		}
		
		@Override
		public String getLevel3Name() {
			return this.level3;
		}
	}
	
	private static final Set<String>	SMART_FOLDERS	= Create.tempSet( Arrays.asList( new String[] {
			"document",
			"driver",
			"firmware",
			"software",
			"image",
			"datasheet",
			"declaration",
			"cli_reference_guide",
			"mib_file",
			"quick_start_guide",
			"support_note",
			"user_guide",								} ) );
	
	static final NameParser createNameParser(final String name, final int fldLuid, final DownloadClient client) {
		final String[] parts = name.toLowerCase().replace( '_', ' ' ).trim().split( " - " );
		switch (parts.length) {
		case 1: {
			final RecFolder folder = client.getFolder( fldLuid );
			if (folder == null) {
				return null;
			}
			if (NameParser.SMART_FOLDERS.contains( folder.getName() )) {
				final RecFolder parent = folder.getParent();
				if (parent != null) {
					return new NameParser.Impl( parent.getName().toLowerCase(), folder.getName(), name );
				}
			}
			return null;
		}
		case 2: {
			final RecFolder folder = client.getFolder( fldLuid );
			if (folder != null) {
				if (NameParser.SMART_FOLDERS.contains( folder.getName() )) {
					final RecFolder parent = folder.getParent();
					if (parent != null) {
						return new NameParser.Impl( parent.getName().toLowerCase(), folder.getName(), name );
					}
				}
			}
			final String level1 = parts[0];
			String level2 = "";
			String level3 = "";
			final String least = parts[1];
			final int posOpen = least.indexOf( " [" );
			if (posOpen == -1) {
				level3 = least;
			} else {
				final int posClose = least.lastIndexOf( ']' );
				final String level2Info = posClose == -1
						? least.substring( posOpen + 2 ).trim()
						: least.substring( posOpen + 2, posClose ).trim();
				if (Character.isDigit( level2Info.charAt( 0 ) )) {
					level2 = level2Info.substring( level2Info.indexOf( ' ' ) + 1 );
				} else {
					level2 = level2Info;
				}
				level3 = least.substring( 0, posOpen );
			}
			return new NameParser.Impl( level1, level2, level3 );
		}
		case 3: {
			if ("soundtracks".equals( parts[0].toLowerCase() )) {
				return new NameParser.Impl( parts[1], "", parts[2], "soundtrack" );
			}
			if ("v_gostyah_u_skazki".equals( parts[0].toLowerCase().replace( ' ', '_' ) )) {
				return new NameParser.Impl( parts[1], "", parts[2], "skazki" );
			}
			return new NameParser.Impl( parts[0], parts[1], parts[2] );
		}
		case 4: {
			if ("soundtracks".equals( parts[0].toLowerCase() )) {
				return new NameParser.Impl( parts[1], "", parts[3], "soundtrack" );
			}
			if ("v_gostyah_u_skazki".equals( parts[0].toLowerCase().replace( ' ', '_' ) )) {
				return new NameParser.Impl( parts[1], "", parts[3], "skazki" );
			}
			return new NameParser.Impl( parts[0], parts[1], parts[3] );
		}
		case 5: {
			if ("soundtracks".equals( parts[0].toLowerCase() )) {
				return new NameParser.Impl( parts[1], parts[2], parts[4], "soundtrack" );
			}
			if ("v_gostyah_u_skazki".equals( parts[0].toLowerCase().replace( ' ', '_' ) )) {
				return new NameParser.Impl( parts[1], parts[2], parts[4], "skazki" );
			}
			return null;
		}
		case 6: {
			return new NameParser.Impl( parts[0], parts[1], parts[3] );
		}
		default:
			return null;
		}
	}
	
	final boolean checkEquals(final RecFile file) {
		if (file == null) {
			return false;
		}
		return this.getLevel1Name().equals( file.getName() )
				&& this.getLevel2Name().equals( file.getLevel2Name() )
				&& this.getLevel3Name().equals( file.getLevel3Name() );
	}
	
	abstract String getHint();
	
	abstract String getLevel1Name();
	
	abstract String getLevel2Name();
	
	abstract String getLevel3Name();
}
