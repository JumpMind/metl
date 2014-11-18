package org.jumpmind.symmetric.is.core.runtime.component;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IComponentFlowChain;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@ComponentDefinition(category = ComponentCategory.SCHEDULER, typeName = "Scheduler", supports = { ComponentSupports.OUTPUT_MESSAGE })
public class SchedulerComponent extends AbstractComponent {

    @SettingDefinition(order = 0, required = true, type = Type.STRING, label = "Cron Expression")
    public final static String CRON_EXPRESSION = "cron.expression";

    String cronExpression;

    ThreadPoolTaskScheduler taskScheduler;

    @Override
    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory,
            ComponentFlowNode componentNode, final IComponentFlowChain chain) {
        super.start(executionTracker, connectionFactory, componentNode, chain);
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setThreadNamePrefix(componentNode.getComponentVersion().getName());
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.initialize();
        this.taskScheduler.schedule(new Runnable() {

            @Override
            public void run() {
                chain.doNext(new Message());
            }
        }, new CronTrigger(componentNode.getComponentVersion().get(CRON_EXPRESSION)));
    }

    @Override
    public void stop() {

    }

}
