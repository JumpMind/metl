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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOutputStream extends OutputStream implements IOutputStreamWithResponse {

    final Logger log = LoggerFactory.getLogger(getClass());

    HttpURLConnection httpUrlConnection;

    OutputStream os;

    StringBuilder response = new StringBuilder();

    public HttpOutputStream(HttpURLConnection httpUrlConnection) {
        this.httpUrlConnection = httpUrlConnection;
        try {
            this.os = this.httpUrlConnection.getOutputStream();
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public String getResponse() {
        return response.toString();
    }

    @Override
    public void write(int b) throws IOException {
        this.os.write(b);
    }

    @Override
    public void flush() throws IOException {
        this.os.flush();
    }

    @Override
    public void close() throws IOException {
        this.os.close();
        BufferedReader in = null;
        int responseCode = -1;
        try {
            responseCode = httpUrlConnection.getResponseCode();
            boolean isError = (responseCode >= 400);
            
            if (isError) {
                if (httpUrlConnection.getErrorStream() != null) {
                    in = new BufferedReader(new InputStreamReader(httpUrlConnection.getErrorStream(), "UTF-8"));
                }
            } else {
                in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream(), "UTF-8"));
            }
            
            if (in != null) {
                String line = in.readLine();
                while (line != null) {
                    response.append(line);
                    response.append(System.getProperty("line.separator"));
                    line = in.readLine();
                }
            }
        } catch (IOException e) {
            throw new IoException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        if (responseCode > 299) {
            throw new IoException(String.format(
                    "Received an unexpected response code of %d with error content of: %s",
                    responseCode, response.toString().replace("%","%%")));
        }

    }

}