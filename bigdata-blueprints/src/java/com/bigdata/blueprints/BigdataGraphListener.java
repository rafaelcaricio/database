/**
Copyright (C) SYSTAP, LLC 2006-2014.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.bigdata.blueprints;



/**
 * Listener interface for a BigdataGraphEmbedded.
 * 
 * @author mikepersonick
 *
 */
public interface BigdataGraphListener {

    void graphEdited(BigdataGraphEdit edit, String raw);

    void transactionBegin();

    void transactionPrepare();

    void transactionCommited(long commitTime);

    void transactionAborted();
    
    public static class BigdataGraphEdit { 
        
        public static enum Action {
            
            Add,
            
            Remove;
            
        }

        private final Action action;
        
        private final String id;
        
        private final String type;
        
        private final String fromId;
        
        private final String toId;
        
        private final String key;
        
        private final Object val;
        
        public BigdataGraphEdit(final Action action, final String id,
                final String type, final String fromId, 
                final String toId, final String key, final Object val) {
            this.action = action;
            this.id = id;
            this.type = type;
            this.fromId = fromId;
            this.toId = toId;
            this.key = key;
            this.val = val;
        }

        public Action getAction() {
            return action;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getFromId() {
            return fromId;
        }

        public String getToId() {
            return toId;
        }

        public String getKey() {
            return key;
        }

        public Object getVal() {
            return val;
        }

        @Override
        public String toString() {
            return "BigdataGraphEdit [action=" + action + ", id=" + id
                    + ", type=" + type + ", fromId=" + fromId
                    + ", toId=" + toId + ", key=" + key + ", val=" + val + "]";
        }

    }
    
}
