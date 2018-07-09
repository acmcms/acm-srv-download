/**
 * 
 */
package ru.myx.srv.download.client.ctrl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.Engine;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecSource;

/**
 * @author myx
 * 
 */
class FormEdit extends AbstractForm<FormEdit> {
	
	private static final BaseObject		STR_TITLE	= MultivariantString.getString( "Server properties",
																Collections.singletonMap( "ru", "Свойства сервера" ) );
	
	private final DownloadClient			parent;
	
	private static final BaseObject		STR_SAVE	= MultivariantString.getString( "Save",
																Collections.singletonMap( "ru", "Сохранить" ) );
	
	private static final ControlCommand<?>	CMD_SAVE	= Control.createCommand( "save", FormEdit.STR_SAVE )
																.setCommandPermission( "modify" )
																.setCommandIcon( "command-create" );
	
	/**
	 * @param parent
	 * @param source
	 */
	FormEdit(final DownloadClient parent, final RecSource source) {
		this.parent = parent;
		final BaseObject data = new BaseNativeObject()//
				.putAppend( "srchost", source.getHost() )//
				.putAppend( "srcport", source.getPort() )//
				.putAppend( "idxhost", source.getHostIndexing() )//
				.putAppend( "idxport", source.getPortIndexing() )//
				.putAppend( "indexing", source.isIndex() )//
				.putAppend( "usage", source.isActive() )//
		;
		this.setData( data );
		this.setAttributeIntern( "id", "edit" );
		this.setAttributeIntern( "title", FormEdit.STR_TITLE );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) throws Exception {
		if (command == FormEdit.CMD_SAVE) {
			final String identity = Base.getString( this.getData(), "identity", "" ).trim();
			final String srcHost = Base.getString( this.getData(), "srchost", "" ).trim();
			final int srcPort = Convert.MapEntry.toInt( this.getData(), "srcport", 80 );
			final String idxHost = Base.getString( this.getData(), "idxhost", srcHost ).trim();
			final int idxPort = Convert.MapEntry.toInt( this.getData(), "idxport", srcPort );
			final boolean index = Convert.MapEntry.toBoolean( this.getData(), "indexing", false );
			final boolean active = Convert.MapEntry.toBoolean( this.getData(), "usage", false );
			try {
				try (final Connection conn = this.parent.nextConnection()) {
					try (final PreparedStatement ps = conn
							.prepareStatement( "UPDATE d1Sources SET srcGuid=?,srcHost=?,srcPort=?,idxHost=?,idxPort=?,srcCreated=?,srcMaintainer=?,srcChecked=?,srcIndex=?,srcActive=?,srcReady=?,srcHealth=? WHERE srcGuid=?" )) {
						ps.setString( 1, identity );
						ps.setString( 2, srcHost );
						ps.setInt( 3, srcPort );
						ps.setString( 4, idxHost );
						ps.setInt( 5, idxPort );
						ps.setTimestamp( 6, new Timestamp( Engine.fastTime() ) );
						ps.setString( 7, "*" );
						ps.setTimestamp( 8, new Timestamp( 0L ) );
						ps.setString( 9, index
								? "Y"
								: "N" );
						ps.setString( 10, active
								? "Y"
								: "N" );
						ps.setInt( 11, 0 );
						ps.setInt( 12, 0 );
						ps.setString( 13, identity );
						ps.execute();
					}
				}
				return null;
			} catch (final SQLException e) {
				throw new RuntimeException( e );
			}
		}
		return super.getCommandResult( command, arguments );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		if (this.parent != null) {
			result.add( FormEdit.CMD_SAVE );
		}
		return result;
	}
	
	@Override
	public final ControlFieldset<?> getFieldset() {
		return FormCreate1.getPropertiesFieldset();
	}
	
}
