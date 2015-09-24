/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.common.accumulators;

import org.apache.flink.util.SerializedValue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AccumulatorHelper {

	/**
	 * Merge two collections of accumulators. The second will be merged into the
	 * first.
	 * 
	 * @param target
	 *            The collection of accumulators that will be updated
	 * @param toMerge
	 *            The collection of accumulators that will be merged into the
	 *            other
	 */
	public static void mergeInto(Map<String, Accumulator<?, ?>> target, Map<String, Accumulator<?, ?>> toMerge) {
		for (Map.Entry<String, Accumulator<?, ?>> otherEntry : toMerge.entrySet()) {
			Accumulator<?, ?> ownAccumulator = target.get(otherEntry.getKey());
			if (ownAccumulator == null) {
				// Create initial counter (copy!)
				target.put(otherEntry.getKey(), otherEntry.getValue().clone());
			}
			else {
				// Both should have the same type
				AccumulatorHelper.compareAccumulatorTypes(otherEntry.getKey(),
						ownAccumulator.getClass(), otherEntry.getValue().getClass());
				// Merge target counter with other counter
				mergeSingle(ownAccumulator, otherEntry.getValue());
			}
		}
	}

	/**
	 * Workaround method for type safety
	 */
	private static <V, R extends Serializable> void mergeSingle(Accumulator<?, ?> target,
															Accumulator<?, ?> toMerge) {
		@SuppressWarnings("unchecked")
		Accumulator<V, R> typedTarget = (Accumulator<V, R>) target;

		@SuppressWarnings("unchecked")
		Accumulator<V, R> typedToMerge = (Accumulator<V, R>) toMerge;

		typedTarget.merge(typedToMerge);
	}

	/**
	 * Compare both classes and throw {@link UnsupportedOperationException} if
	 * they differ
	 */
	@SuppressWarnings("rawtypes")
	public static void compareAccumulatorTypes(Object name,
												Class<? extends Accumulator> first,
												Class<? extends Accumulator> second)
			throws UnsupportedOperationException
	{
		if (first == null || second == null) {
			throw new NullPointerException();
		}

		if (first != second) {
			if (!first.getName().equals(second.getName())) {
				throw new UnsupportedOperationException("The accumulator object '" + name
					+ "' was created with two different types: " + first.getName() + " and " + second.getName());
			} else {
				// damn, name is the same, but different classloaders
				throw new UnsupportedOperationException("The accumulator object '" + name
						+ "' was created with two different classes: " + first + " and " + second
						+ " Both have the same type (" + first.getName() + ") but different classloaders: "
						+ first.getClassLoader() + " and " + second.getClassLoader());
			}
		}
	}

	/**
	 * Transform the Map with accumulators into a Map containing only the
	 * results
	 */
	public static Map<String, Object> toResultMap(Map<String, Accumulator<?, ?>> accumulators) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		for (Map.Entry<String, Accumulator<?, ?>> entry : accumulators.entrySet()) {
			resultMap.put(entry.getKey(), entry.getValue().getLocalValue());
		}
		return resultMap;
	}

	public static String getResultsFormated(Map<String, Object> map) {
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			builder.append("- ").append(entry.getKey()).append(" (").append(entry.getValue().getClass().getName());
			builder.append(")").append(": ").append(entry.getValue().toString()).append("\n");
		}
		return builder.toString();
	}

	public static void resetAndClearAccumulators(Map<String, Accumulator<?, ?>> accumulators) {
		if (accumulators != null) {
			for (Map.Entry<String, Accumulator<?, ?>> entry : accumulators.entrySet()) {
				entry.getValue().resetLocal();
			}
			accumulators.clear();
		}
	}

	public static Map<String, Accumulator<?, ?>> copy(Map<String, Accumulator<?, ?>> accumulators) {
		Map<String, Accumulator<?, ?>> result = new HashMap<String, Accumulator<?, ?>>();

		for(Map.Entry<String, Accumulator<?, ?>> entry: accumulators.entrySet()){
			result.put(entry.getKey(), entry.getValue().clone());
		}

		return result;
	}

	/**
	 * Takes the serialized accumulator results and tries to deserialize them using the provided
	 * class loader.
	 * @param serializedAccumulators The serialized accumulator results.
	 * @param loader The class loader to use.
	 * @return The deserialized accumulator results.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Map<String, Object> deserializeAccumulators(
			Map<String, SerializedValue<Object>> serializedAccumulators, ClassLoader loader)
			throws IOException, ClassNotFoundException {

		if (serializedAccumulators == null || serializedAccumulators.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, Object> accumulators = new HashMap<>(serializedAccumulators.size());

		for (Map.Entry<String, SerializedValue<Object>> entry : serializedAccumulators.entrySet()) {

			Object value = null;
			if (entry.getValue() != null) {
				value = entry.getValue().deserializeValue(loader);
			}

			accumulators.put(entry.getKey(), value);
		}

		return accumulators;
	}

}
