package org.jumpmind.metl.ui.common;

import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroup.CommitHandler;

public class PreCommitHandler implements CommitHandler {

    private static final long serialVersionUID = 1L;
    
    Runnable runnable;

    public PreCommitHandler(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void preCommit(CommitEvent commitEvent) throws CommitException {
        runnable.run();
    }

    @Override
    public void postCommit(CommitEvent commitEvent) throws CommitException {
    }

}
