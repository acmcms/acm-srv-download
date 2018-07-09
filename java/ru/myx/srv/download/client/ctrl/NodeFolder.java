/*
 * Created on 20.10.2004
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ru.myx.srv.download.client.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ru.myx.ae1.control.AbstractNode;
import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.ControlNode;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.ControlBasic;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Format;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecFile;
import ru.myx.srv.download.client.RecFolder;

/**
 * @author myx
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class NodeFolder extends AbstractNode {
	private final DownloadClient			parent;
	
	private final RecFolder					folder;
	
	private static final Object				STR_CREATE_FILE		= MultivariantString.getString( "Upload a file",
																		Collections.singletonMap( "ru",
																				"Загрузить файл" ) );
	
	private static final ControlCommand<?>	CMD_UPLOAD			= Control
																		.createCommand( "upload",
																				NodeFolder.STR_CREATE_FILE )
																		.setCommandPermission( "create" )
																		.setCommandIcon( "command-create" );
	
	private static final Object				STR_COMMENT			= MultivariantString
																		.getString( "Comment", Collections
																				.singletonMap( "ru", "Комментарий" ) );
	
	private static final Object				STR_NAME			= MultivariantString.getString( "Name",
																		Collections.singletonMap( "ru", "Имя" ) );
	
	private static final Object				STR_SIZE			= MultivariantString.getString( "Size",
																		Collections.singletonMap( "ru", "Размер" ) );
	
	private static final ControlFieldset<?>	FIELDSET_LISTING	= ControlFieldset
																		.createFieldset()
																		.addField( ControlFieldFactory
																				.createFieldString( "name",
																						NodeFolder.STR_NAME,
																						"" ) )
																		.addField( ControlFieldFactory
																				.createFieldString( "size",
																						NodeFolder.STR_SIZE,
																						"" ) )
																		.addField( ControlFieldFactory
																				.createFieldInteger( "md5", "MD5", 0 ) )
																		.addField( ControlFieldFactory
																				.createFieldString( "comment",
																						NodeFolder.STR_COMMENT,
																						"" ) );
	
	private static final Object				STR_EDIT			= MultivariantString.getString( "Properties",
																		Collections.singletonMap( "ru", "Свойства" ) );
	
	private static final Object				STR_DELETE			= MultivariantString.getString( "Delete",
																		Collections.singletonMap( "ru", "УДалить" ) );
	
	/**
	 * @param parent
	 * @param folder
	 */
	public NodeFolder(final DownloadClient parent, final RecFolder folder) {
		this.parent = parent;
		this.folder = folder;
		this.setAttributeIntern( "title", this.getKey() );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == NodeFolder.CMD_UPLOAD) {
			return new FormUpload( this.parent );
		}
		if (command.getKey().equals( "delete" )) {
			final String key = Base.getString( command.getAttributes(), "key", "" ).trim();
			return new FormDelete( this.getLocationControl(), this.parent, this.parent.getSource( key ) );
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( NodeFolder.CMD_UPLOAD );
		return result;
	}
	
	@Override
	public ControlCommandset getContentCommands(final String key) {
		final ControlCommandset result = Control.createOptions();
		result.add( Control.createCommand( "edit", NodeFolder.STR_EDIT ).setCommandPermission( "edit" )
				.setCommandIcon( "command-edit" ).setAttribute( "key", key ) );
		result.add( Control.createCommand( "delete", NodeFolder.STR_DELETE ).setCommandPermission( "delete" )
				.setCommandIcon( "command-delete" ).setAttribute( "key", key ) );
		return result;
	}
	
	@Override
	public ControlFieldset<?> getContentFieldset() {
		return NodeFolder.FIELDSET_LISTING;
	}
	
	@Override
	public List<ControlBasic<?>> getContents() {
		final List<ControlBasic<?>> result = new ArrayList<>();
		final Map<String, RecFile> files = this.folder.getFiles();
		for (final Map.Entry<String, RecFile> entry : files.entrySet()) {
			final RecFile file = entry.getValue();
			final BaseObject data = new BaseNativeObject()//
					.putAppend( "name", file.getName() )//
					.putAppend( "md5", file.getMd5() )//
					.putAppend( "comment", file.getComment() )//
					.putAppend( "size", Format.Compact.toBytes( file.getSize() ) )//
			;
			result.add( Control.createBasic( file.getName(), file.getName(), data ) );
		}
		return result;
	}
	
	@Override
	public String getKey() {
		if (this.folder.getParentLuid() == 0) {
			return String.valueOf( this.folder.getSource().getGuid() );
		}
		return this.folder.getName();
	}
	
	@Override
	public final String getLocationControl() {
		return null;
	}
	
	@Override
	public String getTitle() {
		if (this.folder.getParentLuid() == 0) {
			return String.valueOf( this.folder.getSource().getHost() );
		}
		return this.folder.getName() + " (" + this.folder.getLuid() + ')';
	}
	
	@Override
	protected ControlNode<?> internGetChildByName(final String name) {
		final Map<String, RecFolder> folders = this.folder.getFolders();
		if (folders == null || folders.size() == 0) {
			return null;
		}
		final RecFolder folder = folders.get( name );
		if (folder == null) {
			return null;
		}
		return new NodeFolder( this.parent, folder );
	}
	
	@Override
	protected ControlNode<?>[] internGetChildren() {
		final Map<String, RecFolder> folders = this.folder.getFolders();
		if (folders == null || folders.size() == 0) {
			return null;
		}
		final List<ControlNode<?>> result = new ArrayList<>( folders.size() );
		for (final RecFolder folder : folders.values()) {
			result.add( new NodeFolder( this.parent, folder ) );
		}
		return result.toArray( new ControlNode<?>[result.size()] );
	}
	
	@Override
	protected boolean internHasChildren() {
		final Map<String, RecFolder> folders = this.folder.getFolders();
		return folders != null && folders.size() > 0;
	}
}
