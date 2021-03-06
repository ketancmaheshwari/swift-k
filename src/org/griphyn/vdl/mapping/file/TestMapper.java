/*
 * Copyright 2012 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.griphyn.vdl.mapping.file;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.griphyn.vdl.mapping.AbsFile;
import org.griphyn.vdl.mapping.AbstractMapper;
import org.griphyn.vdl.mapping.FileSystemLister;
import org.griphyn.vdl.mapping.Mapper;
import org.griphyn.vdl.mapping.MappingParamSet;
import org.griphyn.vdl.mapping.Path;
import org.griphyn.vdl.mapping.PhysicalFormat;


public class TestMapper extends AbstractMapper {
    
    @Override
    protected void getValidMappingParams(Set<String> s) {
        s.addAll(TestMapperParams.NAMES);
        super.getValidMappingParams(s);
    }
    
    private PhysicalFormat remap, map;    
    
    @Override
    protected MappingParamSet newParams() {
        return new TestMapperParams();
    }

    @Override
    public String getName() {
        return "TestMapper";
    }

    @Override
    public boolean canBeRemapped(Path path) {
        TestMapperParams cp = getParams();
        return cp.getRemappable();
    }

    @Override
    public void remap(Path path, Mapper sourceMapper, Path sourcePath) {
        TestMapperParams cp = getParams();
        if (cp.getRemappable()) {
            remap = sourceMapper.map(sourcePath);
            System.out.println("Remapping " + path + " -> " + remap);
            ensureCollectionConsistency(sourceMapper, sourcePath);
        }
        else {
            throw new UnsupportedOperationException("remap");
        }
    }

    @Override
    public void clean(Path path) {
        TestMapperParams cp = getParams();
        PhysicalFormat pf = map(path);
        if (cp.getTemp()) {
            System.out.println("Cleaning file " + pf);
            FileGarbageCollector.getDefault().decreaseUsageCount(pf);
        }
        else {
            System.out.println("Not cleaning " + pf + " (not temporary)");
        }
    }

    @Override
    public boolean isPersistent(Path path) {
        TestMapperParams cp = getParams();
        return !cp.getTemp();
    }

    public PhysicalFormat map(Path path) {
        TestMapperParams cp = getParams();
        if (remap == null) {
            if (map == null) {
                map = new AbsFile(cp.getFile());
            }
            return map;
        }
        else {
            return remap;
        }
    }

    public Collection<Path> existing() {
        return Collections.singletonList(Path.EMPTY_PATH);
    }
    
    @Override
    public Collection<Path> existing(FileSystemLister l) {
        return Collections.singletonList(Path.EMPTY_PATH);
    }

    public boolean isStatic() {
        TestMapperParams cp = getParams();
        if (cp != null) {
            return cp.getStatic_();
        }
        else {
            return false;
        }
    }
}
