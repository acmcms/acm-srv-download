package ru.myx.srv.download.client.ctrl;

/*
 * Created on 26.01.2006
 */
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

import ru.myx.ae1.control.Control;
import ru.myx.ae1.control.MultivariantString;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.control.AbstractForm;
import ru.myx.ae3.control.command.ControlCommand;
import ru.myx.ae3.control.command.ControlCommandset;
import ru.myx.ae3.control.field.ControlFieldFactory;
import ru.myx.ae3.control.fieldset.ControlFieldset;
import ru.myx.ae3.help.Convert;
import ru.myx.srv.download.client.DownloadClient;
import ru.myx.srv.download.client.RecSource;

/**
 * @author myx
 * 
 */
class FormDelete extends AbstractForm<FormDelete> {
	private static final ControlCommand<?>	UNLINK			= Control.createCommand( "unlink", "Unlink" )
																	.setCommandPermission( "delete" )
																	.setCommandIcon( "command-delete" );
	
	private final DownloadClient			parent;
	
	private final RecSource					source;
	
	private static final ControlFieldset<?>	FIELDSET_DELETE	= ControlFieldset
																	.createFieldset( "confirmation" )
																	.addField( ControlFieldFactory.createFieldBoolean( "confirmation",
																			MultivariantString.getString( "yes, i do.",
																					Collections.singletonMap( "ru",
																							"однозначно" ) ),
																			false ) );
	
	FormDelete(final String locationControl, final DownloadClient parent, final RecSource source) {
		this.parent = parent;
		this.source = source;
		this.setAttributeIntern( "id", "confirmation" );
		this.setAttributeIntern( "path", locationControl );
		this.setAttributeIntern( "title",
				MultivariantString.getString( "Do you really want to delete this entry?",
						Collections.singletonMap( "ru", "Вы действительно хотите удалить эту запись?" ) ) );
		this.recalculate();
	}
	
	@Override
	public Object getCommandResult(final ControlCommand<?> command, final BaseObject arguments) {
		if (command == FormDelete.UNLINK) {
			if (Convert.MapEntry.toBoolean( this.getData(), "confirmation", false )) {
				try {
					try (final Connection conn = this.parent.nextConnection()) {
						conn.setAutoCommit( false );
						try {
							{
								try (final PreparedStatement ps = conn
										.prepareStatement( "DELETE FROM d1ItemLinkage WHERE itmGuid in (SELECT itmGuid FROM d1Items WHERE fldLuid in (SELECT fldLuid FROM d1Folders WHERE srcLuid in (SELECT srcLuid FROM d1Sources WHERE srcGuid=?)))" )) {
									ps.setString( 1, this.source.getGuid() );
									ps.execute();
								}
							}
							{
								try (final PreparedStatement ps = conn
										.prepareStatement( "DELETE FROM d1Items WHERE fldLuid in (SELECT fldLuid FROM d1Folders WHERE srcLuid in (SELECT srcLuid FROM d1Sources WHERE srcGuid=?))" )) {
									ps.setString( 1, this.source.getGuid() );
									ps.execute();
								}
							}
							{
								try (final PreparedStatement ps = conn
										.prepareStatement( "DELETE FROM d1Folders WHERE srcLuid in (SELECT srcLuid FROM d1Sources WHERE srcGuid=?)" )) {
									ps.setString( 1, this.source.getGuid() );
									ps.execute();
								}
							}
							{
								try (final PreparedStatement ps = conn
										.prepareStatement( "DELETE FROM d1Sources WHERE srcGuid=?" )) {
									ps.setString( 1, this.source.getGuid() );
									ps.execute();
								}
							}
							conn.commit();
						} finally {
							try {
								conn.rollback();
							} catch (final Throwable t) {
								// ignore
							}
						}
					}
				} catch (final SQLException e) {
					throw new RuntimeException( e );
				}
				return null;
			}
			return "Action cancelled.";
		}
		throw new IllegalArgumentException( "Unknown command: " + command.getKey() );
	}
	
	@Override
	public ControlCommandset getCommands() {
		final ControlCommandset result = Control.createOptions();
		result.add( FormDelete.UNLINK );
		return result;
	}
	
	@Override
	public ControlFieldset<?> getFieldset() {
		return FormDelete.FIELDSET_DELETE;
	}
}
