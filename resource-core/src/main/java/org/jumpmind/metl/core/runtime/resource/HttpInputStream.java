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
package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class HttpInputStream extends InputStream implements IInputStreamWithConnection {

    final Logger log = LoggerFactory.getLogger(getClass());

    HttpURLConnection httpUrlConnection;

    InputStream is;

    StringBuilder response = new StringBuilder();

    public HttpInputStream(HttpURLConnection httpUrlConnection) {
        this.httpUrlConnection = httpUrlConnection;
        try {
            this.is = this.httpUrlConnection.getInputStream();
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public HttpURLConnection getHttpConnection() {
        return httpUrlConnection;
    }

    @Override
    public int read() throws IOException {
        return this.is.read();
    }

    @Override
    public InputStream getInputStream() {
        return is;
    }
}
