/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import java.util.Collections;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.Loadable;

/**
 * EntityAliases which handles the logic of selecting user provided aliases (via return-property),
 * before using the default aliases.
 *
 * @author max
 *
 */
public class DefaultEntityAliases implements EntityAliases {

	private static final String[][] EMPTY_ARRAY_OF_ARRAY_OF_STRINGS = new String[0][];

	private final String[] suffixedKeyColumns;
	private final String[] suffixedVersionColumn;
	private final String[][] suffixedPropertyColumns;
	private final String suffixedDiscriminatorColumn;
	private final String suffix;
	private final String rowIdAlias;
	private final Map userProvidedAliases;

	/**
	 * Calculate and cache select-clause aliases
	 *
	 * @param userProvidedAliases The explicit aliases provided in a result-set mapping.
	 * @param persister The persister for which we are generating select aliases
	 * @param suffix The calculated suffix.
	 */
	public DefaultEntityAliases(
			Map userProvidedAliases,
			Loadable persister,
			String suffix) {
		this.suffix = suffix.intern();
		this.userProvidedAliases = userProvidedAliases;

		suffixedKeyColumns = determineKeyAlias( persister, suffix );
		suffixedPropertyColumns = determinePropertyAliases( persister );
		suffixedDiscriminatorColumn = determineDiscriminatorAlias( persister, suffix );
		suffixedVersionColumn = determineVersionAlias( persister );
		rowIdAlias = (Loadable.ROWID_ALIAS + suffix).intern(); // TODO: not visible to the user!
	}

	public DefaultEntityAliases(Loadable persister, String suffix) {
		this( Collections.EMPTY_MAP, persister, suffix );
	}

	private String[] determineKeyAlias(Loadable persister, String suffix) {
		final String[] aliases;
		final String[] keyColumnsCandidates = getUserProvidedAliases( persister.getIdentifierPropertyName(), null );
		if ( keyColumnsCandidates == null ) {
			aliases = getUserProvidedAliases(
					"id",
					getIdentifierAliases(persister, suffix)
			);
		}
		else {
			aliases = keyColumnsCandidates;
		}
		final String[] rtn = StringHelper.unquote( aliases, persister.getFactory().getDialect() );
		intern( rtn );
		return rtn;
	}

	private String[][] determinePropertyAliases(Loadable persister) {
		return getSuffixedPropertyAliases( persister );
	}

	private String determineDiscriminatorAlias(Loadable persister, String suffix) {
		String alias = getUserProvidedAlias( "class", getDiscriminatorAlias( persister, suffix ) );
		return StringHelper.unquote( alias, persister.getFactory().getDialect() );
	}

	private String[] determineVersionAlias(Loadable persister) {
		return persister.isVersioned()
				? suffixedPropertyColumns[ persister.getVersionProperty() ]
				: null;
	}

	protected String getDiscriminatorAlias(Loadable persister, String suffix) {
		return persister.getDiscriminatorAlias(suffix);
	}

	protected String[] getIdentifierAliases(Loadable persister, String suffix) {
		return persister.getIdentifierAliases(suffix);
	}

	protected String[] getPropertyAliases(Loadable persister, int j) {
		return persister.getPropertyAliases(suffix, j);
	}

	private String[] getUserProvidedAliases(String propertyPath, String[] defaultAliases) {
		String[] result = (String[]) userProvidedAliases.get(propertyPath);
		if (result==null) {
			return defaultAliases;
		}
		else {
			return result;
		}
	}

	private String getUserProvidedAlias(String propertyPath, String defaultAlias) {
		String[] columns = (String[]) userProvidedAliases.get(propertyPath);
		if (columns==null) {
			return defaultAlias;
		}
		else {
			return columns[0];
		}
	}

	@Override
	public String[][] getSuffixedPropertyAliases(Loadable persister) {
		final int size = persister.getPropertyNames().length;
		final String[][] suffixedPropertyAliases;
		if (size > 0) {
			suffixedPropertyAliases = new String[size][];
			for ( int j = 0; j < size; j++ ) {
				suffixedPropertyAliases[j] = getUserProvidedAliases(
						persister.getPropertyNames()[j],
						getPropertyAliases( persister, j )
				);
				suffixedPropertyAliases[j] = StringHelper.unquote( suffixedPropertyAliases[j], persister.getFactory().getDialect() );
				intern( suffixedPropertyAliases[j] );
			}
		}
		else {
			suffixedPropertyAliases = EMPTY_ARRAY_OF_ARRAY_OF_STRINGS;
		}
		return suffixedPropertyAliases;
	}

	@Override
	public String[] getSuffixedVersionAliases() {
		return suffixedVersionColumn;
	}

	@Override
	public String[][] getSuffixedPropertyAliases() {
		return suffixedPropertyColumns;
	}

	@Override
	public String getSuffixedDiscriminatorAlias() {
		return suffixedDiscriminatorColumn;
	}

	@Override
	public String[] getSuffixedKeyAliases() {
		return suffixedKeyColumns;
	}

	@Override
	public String getRowIdAlias() {
		return rowIdAlias;
	}

	@Override
	public String getSuffix() {
		return suffix;
	}

	private static void intern(String[] strings) {
		for ( int i = 0; i < strings.length; i++ ) {
			strings[i] = strings[i].intern();
		}
	}
}
