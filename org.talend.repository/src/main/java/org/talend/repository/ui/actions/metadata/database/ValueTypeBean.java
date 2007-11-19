// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend – www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.ui.actions.metadata.database;

/**
 * DOC ggu class global comment. Detailled comment <br/>
 * 
 * $Id: talend.epf 1 2006-09-29 17:06:40 +0000 (ææäº, 29 ä¹æ 2006) nrousseau $
 * 
 */
public class ValueTypeBean {

    private String connName;

    private String value;

    /**
     * DOC ggu ValueTypeBean constructor comment.
     * 
     * @param connName
     * @param value
     */
    public ValueTypeBean(String connName, String value) {
        super();
        this.connName = connName;
        this.value = value;
    }

    /**
     * Getter for connName.
     * 
     * @return the connName
     */
    public String getConnName() {
        return this.connName;
    }

    /**
     * Getter for value.
     * 
     * @return the value
     */
    public String getValue() {
        return this.value;
    }

}
