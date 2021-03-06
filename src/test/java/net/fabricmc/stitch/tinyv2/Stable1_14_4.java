/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.stitch.tinyv2;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.ParameterDef;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

public class Stable1_14_4 {
	private static final String DIR = new File(Stable1_14_4.class.getClassLoader().getResource("stable-1.14.4").getPath()).getAbsolutePath() + "/";

	@Test
	public void testReorder2() throws Exception {
		Commands.reorder(DIR + "intermediary-mappings.tinyv2",
						DIR + "intermediary-mappings-inverted.tinyv2",
						"intermediary", "official"
		);
	}

	@Test
	@Disabled
	public void testMerge() throws Exception {
		String target = DIR + "merged-unordered.tinyv2";
		Commands.merge(DIR + "intermediary-mappings-inverted.tinyv2",
						DIR + "yarn-mappings.tinyv2",
						target
		);
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(target))) {
			TinyTree mappings = TinyMappingFactory.load(reader);

			ParameterDef blockInitParam = findMethodParameterMapping("intermediary", "net/minecraft/class_2248",
							"<init>", "(Lnet/minecraft/class_2248$class_2251;)V", 1, mappings);

			Assertions.assertEquals("settings", blockInitParam.getName("named"));

		}

	}

	private ClassDef findClassMapping(String column, String key, TinyTree mappings) {
		return find(mappings.getClasses(), c -> c.getName(column).equals(key))
						.orElseThrow(() -> new AssertionError("Could not find key " + key + " in namespace " + column));
	}

	private MethodDef findMethodMapping(String column, String className, String methodName, String descriptor, TinyTree mappings) {
		return find(findClassMapping(column, className, mappings).getMethods(),
						m -> m.getName(column).equals(methodName) && m.getDescriptor(column).equals(descriptor)

		).orElseThrow(() -> new AssertionError("Could not find key " + className + " " + descriptor + " " + methodName + " in namespace " + column));
	}


	private ParameterDef findMethodParameterMapping(String column, String className, String methodName, String descriptor,
													int lvIndex, TinyTree mappings) {
		MethodDef method = findMethodMapping(column, className, methodName, descriptor, mappings);
		return find(method.getParameters(),
						p -> p.getLocalVariableIndex() == lvIndex)
						.orElseThrow(() -> new AssertionError("Could not find key" + className + " " + descriptor + " " + methodName + " " + lvIndex + " in namespace " + column));
	}

	private <T> Optional<T> find(Collection<T> list, Predicate<T> predicate) {
		return list.stream().filter(predicate).findFirst();
	}


	@Test
	@Disabled
	public void testReorder3() throws Exception {
		Commands.reorder(DIR + "merged-unordered.tinyv2",
						DIR + "merged.tinyv2",
						"official", "intermediary", "named"
		);
	}

	@Test
	@Disabled
	public void testFieldNameProposal() throws Exception {
		Commands.proposeFieldNames("local/1.14.4-merged.jar",
						DIR + "merged.tinyv2", DIR + "merged-proposed.tinyv2");
	}

	// Requirements:
	// - Official -> Intermediary mappings "intermediary-mappings.tinyv2" from enigma
	// - Intermediary -> Named mappings "yarn-mappings.tinyv2" from yarn
	// - 1.14.4 merged jar from yarn at local/1.14.4-merge.jar
	@Test
	@Disabled
	public void testFullProcess() throws Exception {
		testReorder2();
		testMerge();
		testReorder3();
		testFieldNameProposal();
	}
}
