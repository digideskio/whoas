package com.github.lookout.whoas;


public interface QueueAction {
    public void call(HookRequest request) throws Exception;
}
