package triggerredeploy.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

/**
 * Re-deploy event handler
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class TriggerRedeployHandler extends AbstractHandler {
	
	private static String webXmlRelPath = "src/main/webapp/WEB-INF/web.xml";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		Job job = new Job("Server clean trigger") {
			
			@Override
			protected IStatus run(IProgressMonitor progressMonitor) {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				
				boolean noWebXml = true;

				for (IProject project : root.getProjects()) {

					IPath webXmlPath = project.getFullPath().append(webXmlRelPath);
					boolean exists = root.exists(webXmlPath);
					if (exists) {
						IFile webXml = project.getFile(webXmlRelPath);

						try {
							webXml.touch(new NullProgressMonitor());
							
							noWebXml = false;
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
				
				boolean noRunningServer = true;
				
				if(!noWebXml){
					IServer[] servers = ServerCore.getServers();
					for(IServer server : servers){
						if(server.canPublish().isOK() && server.getServerState() == IServer.STATE_STARTED){
							
							SubMonitor subMonitor = SubMonitor.convert(progressMonitor, 90);
							
							IStatus publish = server.publish(IServer.PUBLISH_CLEAN, subMonitor);
							
							if(!publish.isOK()){
								return new Status(publish.getSeverity(), "TriggerRedeploy", "Publis status " + publish.getMessage());
							}
							
							noRunningServer = false;
						}
					}
				}
				
				if(noWebXml){
					return new Status(IStatus.WARNING, "TriggerRedeploy", "No web XML");
				}
				if(noRunningServer){
					return new Status(IStatus.WARNING, "TriggerRedeploy", "No started server");
				}
				return new Status(IStatus.OK, "TriggerRedeploy", "Server clean triggered");
			}
		};

		job.schedule();

		return null;
	}
	
}
