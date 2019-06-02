/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.model;

public enum DataType {

    BIT, BOOLEAN, TINYINT, SMALLINT, INTEGER, BIGINT, FLOAT, REAL, DOUBLE, NUMERIC, DECIMAL, CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR, DATE, TIME, TIMESTAMP, BINARY, VARBINARY, LONGVARBINARY, NULL, OTHER, JAVA_OBJECT, DISTINCT, STRUCT, BLOB, CLOB, NCLOB, DATALINK, ROWID, SQLXML;

    public boolean isBoolean() {
        return this.equals(BIT) || this.equals(BOOLEAN);
    }

    public boolean isNumeric() {
        return this.equals(TINYINT) || this.equals(SMALLINT) || this.equals(INTEGER) || this.equals(BIGINT) || this.equals(DECIMAL)
                || this.equals(NUMERIC) || this.equals(FLOAT) || this.equals(DOUBLE);
    }

    public boolean isTimestamp() {
        return this.equals(DATE) || this.equals(TIME) || this.equals(TIMESTAMP);
    }

    public boolean isString() {
        return this.equals(CHAR) || this.equals(VARCHAR) || this.equals(LONGVARCHAR) || this.equals(NCHAR) || this.equals(NVARCHAR)
                || this.equals(LONGNVARCHAR) || this.equals(CLOB) || this.equals(NCLOB);
    }

    public boolean isBinary() {
        return this.equals(BINARY) || this.equals(VARBINARY) || this.equals(LONGVARBINARY) || this.equals(BLOB);
    }

}
