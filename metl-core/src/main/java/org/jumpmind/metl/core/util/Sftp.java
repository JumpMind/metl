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
package org.jumpmind.metl.core.util;

import javax.swing.JOptionPane;

/**
 * This program will demonstrate the sftp protocol support.
 * You will be asked username, host, port and passwd. 
 * If everything works fine, you will get a prompt 'sftp>'. 
 * 'help' command will show available command.
 * In current implementation, the destination path for 'get' and 'put'
 * commands must be a file, not a directory.
 *
 */
import com.jcraft.jsch.*;
import javax.swing.*;

public class Sftp{
	public static void main(String[] arg){
		// Automatically calculates whether you have a windowing system or not, and will popup windows or command line.
		// java -cp metl-core-3.4.2.jar:/Users/philipmarzullo/metl/metl-3.3.0.r.84/WEB-INF/lib/jsch-0.1.54.jar org.jumpmind.metl.core.util.Sftp
		// Force to use command line only.
		// java -cp metl-core-3.4.2.jar:/Users/philipmarzullo/metl/metl-3.3.0.r.84/WEB-INF/lib/jsch-0.1.54.jar -Djava.awt.headless=true org.jumpmind.metl.core.util.Sftp
		try{
			JSch jsch=new JSch();

			String host=null;
			if(arg.length>0){
				host=arg[0];
			}
			else{
				host=SftpUtils.showInputDialog("Enter username@hostname:port",
						System.getProperty("user.name")+
						"@localhost:22"); 
			}
			String user=host.substring(0, host.indexOf('@'));
			int port=22;
			if(host.indexOf(":") >= 0) {
				port = Integer.valueOf(host.substring(host.indexOf(":")+1));
				host = host.substring(host.indexOf('@')+1,host.indexOf(":"));
			} else {
				host=host.substring(host.indexOf('@')+1);
			}

			Session session=jsch.getSession(user, host, port);

			// username and password will be given via UserInfo interface.
			UserInfo ui=new MyUserInfo();
			session.setUserInfo(ui);

			session.connect();

			Channel channel=session.openChannel("sftp");
			channel.connect();
			ChannelSftp c=(ChannelSftp)channel;

			java.io.InputStream in=System.in;
			java.io.PrintStream out=System.out;

			java.util.Vector<String> cmds=new java.util.Vector<String>();
			byte[] buf=new byte[1024];
			int i;
			String str;
			int level=0;

			while(true){
				out.print("sftp> ");
				cmds.removeAllElements();
				i=in.read(buf, 0, 1024);
				if(i<=0)break;

				i--;
				if(i>0 && buf[i-1]==0x0d)i--;
				//str=new String(buf, 0, i);
				//System.out.println("|"+str+"|");
				int s=0;
				for(int ii=0; ii<i; ii++){
					if(buf[ii]==' '){
						if(ii-s>0){ cmds.addElement(new String(buf, s, ii-s)); }
						while(ii<i){if(buf[ii]!=' ')break; ii++;}
						s=ii;
					}
				}
				if(s<i){ cmds.addElement(new String(buf, s, i-s)); }
				if(cmds.size()==0)continue;

				String cmd=(String)cmds.elementAt(0);
				if(cmd.equals("quit")){
					c.quit();
					break;
				}
				if(cmd.equals("exit")){
					c.exit();
					break;
				}
				if(cmd.equals("rekey")){
					session.rekey();
					continue;
				}
				if(cmd.equals("compression")){
					if(cmds.size()<2){
						out.println("compression level: "+level);
						continue;
					}
					try{
						level=Integer.parseInt((String)cmds.elementAt(1));
						if(level==0){
							session.setConfig("compression.s2c", "none");
							session.setConfig("compression.c2s", "none");
						}
						else{
							session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
							session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
						}
					}
					catch(Exception e){}
					session.rekey();
					continue;
				}
				if(cmd.equals("cd") || cmd.equals("lcd")){
					if(cmds.size()<2) continue;
					String path=(String)cmds.elementAt(1);
					try{
						if(cmd.equals("cd")) c.cd(path);
						else c.lcd(path);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("rm") || cmd.equals("rmdir") || cmd.equals("mkdir")){
					if(cmds.size()<2) continue;
					String path=(String)cmds.elementAt(1);
					try{
						if(cmd.equals("rm")) c.rm(path);
						else if(cmd.equals("rmdir")) c.rmdir(path);
						else c.mkdir(path);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("chgrp") || cmd.equals("chown") || cmd.equals("chmod")){
					if(cmds.size()!=3) continue;
					String path=(String)cmds.elementAt(2);
					int foo=0;
					if(cmd.equals("chmod")){
						byte[] bar=((String)cmds.elementAt(1)).getBytes();
						int k;
						for(int j=0; j<bar.length; j++){
							k=bar[j];
							if(k<'0'||k>'7'){foo=-1; break;}
							foo<<=3;
							foo|=(k-'0');
						}
						if(foo==-1)continue;
					}
					else{
						try{foo=Integer.parseInt((String)cmds.elementAt(1));}
						catch(Exception e){continue;}
					}
					try{
						if(cmd.equals("chgrp")){ c.chgrp(foo, path); }
						else if(cmd.equals("chown")){ c.chown(foo, path); }
						else if(cmd.equals("chmod")){ c.chmod(foo, path); }
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("pwd") || cmd.equals("lpwd")){
					str=(cmd.equals("pwd")?"Remote":"Local");
					str+=" working directory: ";
					if(cmd.equals("pwd")) str+=c.pwd();
					else str+=c.lpwd();
					out.println(str);
					continue;
				}
				if(cmd.equals("ls") || cmd.equals("dir")){
					String path=".";
					if(cmds.size()==2) path=(String)cmds.elementAt(1);
					try{
						java.util.Vector vv=c.ls(path);
						if(vv!=null){
							for(int ii=0; ii<vv.size(); ii++){
								//		out.println(vv.elementAt(ii).toString());

								Object obj=vv.elementAt(ii);
								if(obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry){
									out.println(((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getLongname());
								}

							}
						}
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("lls") || cmd.equals("ldir")){
					String path=".";
					if(cmds.size()==2) path=(String)cmds.elementAt(1);
					try{
						java.io.File file=new java.io.File(path);
						if(!file.exists()){
							out.println(path+": No such file or directory");
							continue; 
						}
						if(file.isDirectory()){
							String[] list=file.list();
							for(int ii=0; ii<list.length; ii++){
								out.println(list[ii]);
							}
							continue;
						}
						out.println(path);
					}
					catch(Exception e){
						System.out.println(e);
					}
					continue;
				}
				if(cmd.equals("get") || 
						cmd.equals("get-resume") || cmd.equals("get-append") || 
						cmd.equals("put") || 
						cmd.equals("put-resume") || cmd.equals("put-append")
						){
					if(cmds.size()!=2 && cmds.size()!=3) continue;
					String p1=(String)cmds.elementAt(1);
					//	  String p2=p1;
					String p2=".";
					if(cmds.size()==3)p2=(String)cmds.elementAt(2);
					try{
						SftpProgressMonitor monitor=new MyProgressMonitor();
						if(cmd.startsWith("get")){
							int mode=ChannelSftp.OVERWRITE;
							if(cmd.equals("get-resume")){ mode=ChannelSftp.RESUME; }
							else if(cmd.equals("get-append")){ mode=ChannelSftp.APPEND; } 
							c.get(p1, p2, monitor, mode);
						}
						else{ 
							int mode=ChannelSftp.OVERWRITE;
							if(cmd.equals("put-resume")){ mode=ChannelSftp.RESUME; }
							else if(cmd.equals("put-append")){ mode=ChannelSftp.APPEND; } 
							c.put(p1, p2, monitor, mode); 
						}
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("ln") || cmd.equals("symlink") ||
						cmd.equals("rename") || cmd.equals("hardlink")){
					if(cmds.size()!=3) continue;
					String p1=(String)cmds.elementAt(1);
					String p2=(String)cmds.elementAt(2);
					try{
						if(cmd.equals("hardlink")){  c.hardlink(p1, p2); }
						else if(cmd.equals("rename")) c.rename(p1, p2);
						else c.symlink(p1, p2);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("df")){
					if(cmds.size()>2) continue;
					String p1 = cmds.size()==1 ? ".": (String)cmds.elementAt(1);
					SftpStatVFS stat = c.statVFS(p1);

					long size = stat.getSize();
					long used = stat.getUsed();
					long avail = stat.getAvailForNonRoot();
					long root_avail = stat.getAvail();
					long capacity = stat.getCapacity();

					System.out.println("Size: "+size);
					System.out.println("Used: "+used);
					System.out.println("Avail: "+avail);
					System.out.println("(root): "+root_avail);
					System.out.println("%Capacity: "+capacity);

					continue;
				}	
				if(cmd.equals("stat") || cmd.equals("lstat")){
					if(cmds.size()!=2) continue;
					String p1=(String)cmds.elementAt(1);
					SftpATTRS attrs=null;
					try{
						if(cmd.equals("stat")) attrs=c.stat(p1);
						else attrs=c.lstat(p1);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					if(attrs!=null){
						out.println(attrs);
					}
					else{
					}
					continue;
				}
				if(cmd.equals("readlink")){
					if(cmds.size()!=2) continue;
					String p1=(String)cmds.elementAt(1);
					String filename=null;
					try{
						filename=c.readlink(p1);
						out.println(filename);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("realpath")){
					if(cmds.size()!=2) continue;
					String p1=(String)cmds.elementAt(1);
					String filename=null;
					try{
						filename=c.realpath(p1);
						out.println(filename);
					}
					catch(SftpException e){
						System.out.println(e.toString());
					}
					continue;
				}
				if(cmd.equals("version")){
					out.println("SFTP protocol version "+c.version());
					continue;
				}
				if(cmd.equals("help") || cmd.equals("?")){
					out.println(help);
					continue;
				}
				out.println("unimplemented command: "+cmd);
			}
			session.disconnect();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		System.exit(0);
	}

	public static class MyUserInfo implements UserInfo, UIKeyboardInteractive{
		public String getPassword(){ return passwd; }
		public boolean promptYesNo(String str){
			Object[] options={ "yes", "no" };
			int foo=SftpUtils.showOptionDialog(
					str,
					"Warning", 
					JOptionPane.DEFAULT_OPTION, 
					JOptionPane.WARNING_MESSAGE,
					null, options, options[0]);
			foo = SftpUtils.showOptionDialog(str, "Warning", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			return foo==0;
		}

		String passwd;

		public String getPassphrase(){ return null; }
		public boolean promptPassphrase(String message){ return true; }
		public boolean promptPassword(String message){
			passwd = SftpUtils.promptPassword(message);
			if(passwd != null && passwd.length() > 0) {
				return true;
			} else {
				return false;
			}
		}
		public void showMessage(String message){
			JOptionPane.showMessageDialog(null, message);
		}
		public String[] promptKeyboardInteractive(String destination,
				String name,
				String instruction,
				String[] prompt,
				boolean[] echo)
		{
			return SftpUtils.promptKeyboardInteractive(destination, name, instruction, prompt, echo);
		}
	}

	public static class MyProgressMonitor implements SftpProgressMonitor{
		ProgressMonitor monitor;
		long count=0;
		long max=0;
		public void init(int op, String src, String dest, long max){
			this.max=max;
			count=0;
			percent=-1;
			if(SftpUtils.isHeadless()) {

			} else {
				monitor=new ProgressMonitor(null, 
						((op==SftpProgressMonitor.PUT)? 
								"put" : "get")+": "+src, 
						"",  0, (int)max);

				monitor.setProgress((int)this.count);
				monitor.setMillisToDecideToPopup(1000);
			}
		}
		private long percent=-1;
		public boolean count(long count){
			this.count+=count;

			if(percent>=this.count*100/max) {
				return true;
			}
			percent=this.count*100/max;

			String message = "Completed "+this.count+"("+percent+"%) out of "+max+".";
			if(SftpUtils.isHeadless()) {
				SftpUtils.showNote(message);
				return true;
			} else {
				monitor.setNote(message);     
				monitor.setProgress((int)this.count);
				return !(monitor.isCanceled());
			}


		}
		public void end(){
			if(SftpUtils.isHeadless()) {
				// Nothing to do
			} else {
				monitor.close();
			}
		}
	}

	private static String help =
			"      Available commands:\n"+
					"      * means unimplemented command.\n"+
					"cd path                       Change remote directory to 'path'\n"+
					"lcd path                      Change local directory to 'path'\n"+
					"chgrp grp path                Change group of file 'path' to 'grp'\n"+
					"chmod mode path               Change permissions of file 'path' to 'mode'\n"+
					"chown own path                Change owner of file 'path' to 'own'\n"+
					"df [path]                     Display statistics for current directory or\n"+
					"                              filesystem containing 'path'\n"+
					"help                          Display this help text\n"+
					"get remote-path [local-path]  Download file\n"+
					"get-resume remote-path [local-path]  Resume to download file.\n"+
					"get-append remote-path [local-path]  Append remote file to local file\n"+
					"hardlink oldpath newpath      Hardlink remote file\n"+
					"*lls [ls-options [path]]      Display local directory listing\n"+
					"ln oldpath newpath            Symlink remote file\n"+
					"*lmkdir path                  Create local directory\n"+
					"lpwd                          Print local working directory\n"+
					"ls [path]                     Display remote directory listing\n"+
					"*lumask umask                 Set local umask to 'umask'\n"+
					"mkdir path                    Create remote directory\n"+
					"put local-path [remote-path]  Upload file\n"+
					"put-resume local-path [remote-path]  Resume to upload file\n"+
					"put-append local-path [remote-path]  Append local file to remote file.\n"+
					"pwd                           Display remote working directory\n"+
					"stat path                     Display info about path\n"+
					"exit                          Quit sftp\n"+
					"quit                          Quit sftp\n"+
					"rename oldpath newpath        Rename remote file\n"+
					"rmdir path                    Remove remote directory\n"+
					"rm path                       Delete remote file\n"+
					"symlink oldpath newpath       Symlink remote file\n"+
					"readlink path                 Check the target of a symbolic link\n"+
					"realpath path                 Canonicalize the path\n"+
					"rekey                         Key re-exchanging\n"+
					"compression level             Packet compression will be enabled\n"+
					"version                       Show SFTP version\n"+
					"?                             Synonym for help";
}