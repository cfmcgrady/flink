/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.streaming.api.windowing.triggers;

import com.google.common.annotations.VisibleForTesting;
import org.apache.flink.streaming.api.windowing.windows.Window;

public class PurgingTrigger<T, W extends Window> implements Trigger<T, W> {
	private static final long serialVersionUID = 1L;

	private Trigger<T, W> nestedTrigger;

	private  PurgingTrigger(Trigger<T, W> nestedTrigger) {
		this.nestedTrigger = nestedTrigger;
	}

	@Override
	public TriggerResult onElement(T element, long timestamp, W window, TriggerContext ctx) {
		TriggerResult triggerResult = nestedTrigger.onElement(element, timestamp, window, ctx);
		switch (triggerResult) {
			case FIRE:
				return TriggerResult.FIRE_AND_PURGE;
			case FIRE_AND_PURGE:
				return TriggerResult.FIRE_AND_PURGE;
			default:
				return TriggerResult.CONTINUE;
		}
	}

	@Override
	public TriggerResult onTime(long time, TriggerContext ctx) {
		TriggerResult triggerResult = nestedTrigger.onTime(time, ctx);
		switch (triggerResult) {
			case FIRE:
				return TriggerResult.FIRE_AND_PURGE;
			case FIRE_AND_PURGE:
				return TriggerResult.FIRE_AND_PURGE;
			default:
				return TriggerResult.CONTINUE;
		}
	}

	@Override
	public Trigger<T, W> duplicate() {
		return new PurgingTrigger<>(nestedTrigger.duplicate());
	}

	@Override
	public String toString() {
		return "PurgingTrigger(" + nestedTrigger.toString() + ")";
	}

	public static <T, W extends Window> PurgingTrigger<T, W> of(Trigger<T, W> nestedTrigger) {
		return new PurgingTrigger<>(nestedTrigger);
	}

	@VisibleForTesting
	public Trigger<T, W> getNestedTrigger() {
		return nestedTrigger;
	}
}
