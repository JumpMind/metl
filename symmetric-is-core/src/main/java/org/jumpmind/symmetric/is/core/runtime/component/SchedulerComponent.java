package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;


@ComponentDefinition(category = ComponentCategory.SCHEDULER, typeName = "Scheduler", supports = { ComponentSupports.OUTPUT_MESSAGE })
public class SchedulerComponent extends AbstractComponent {

	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ComponentStatistics getComponentStatistcics() {
		// TODO Auto-generated method stub
		return null;
	}

	//TODO: THIS SHOULD COMPLETELY GO AWAY
	
//    @SettingDefinition(order = 0, required = true, type = Type.BOOLEAN, defaultValue = "false", label = "Enabled")
//    public final static String ENABLED = "scheduler.enabled";
//
//    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Cron Expression")
//    public final static String CRON_EXPRESSION = "cron.expression";
//
//    ThreadPoolTaskScheduler taskScheduler;
//
//    @Override
//    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory,
//            ComponentFlowNode componentNode, final IComponentFlowChain chain) {
//        super.start(executionTracker, connectionFactory, componentNode, chain);
//        TypedProperties properties = componentNode.getComponentVersion().toTypedProperties(this,
//                false);
//        if (properties.is(ENABLED)) {
//            String cron = properties.get(CRON_EXPRESSION);
//            log.info("Starting scheduler with a cron expression of '{}' for '{}':{}", new Object[] {
//                    cron, componentNode.getComponentVersion().getName(),
//                    componentNode.getComponentVersion().getId() });
//
//            this.taskScheduler = new ThreadPoolTaskScheduler();
//            this.taskScheduler.setThreadNamePrefix(componentNode.getComponentVersion().getName());
//            this.taskScheduler.setPoolSize(1);
//            this.taskScheduler.initialize();
//            this.taskScheduler.schedule(new Runnable() {
//
//                @Override
//                public void run() {
//                    chain.doNext(new Message());
//                }
//            }, new CronTrigger(cron));
//        } else {
//            log.info("Did not start scheduler for '{}':{}.  Is is not enabled.", new Object[] {
//                    componentNode.getComponentVersion().getName(),
//                    componentNode.getComponentVersion().getId() });
//        }
//    }
//
//    @Override
//    public void stop() {
//        if (taskScheduler != null) {
//            this.taskScheduler.destroy();
//            this.taskScheduler = null;
//        }
//    }

}
