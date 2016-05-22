/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.coinblesk.server.controller;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.coinblesk.json.Type;
import com.coinblesk.json.VersionTO;
import com.coinblesk.server.utils.ApiVersion;
import com.coinblesk.util.CoinbleskException;

/**
 * @author Andreas Albrecht
 */
@RestController
@RequestMapping(value = {"/version", "/v"})
@ApiVersion({"v1", ""})
public class VersionController {
	
	private static Set<String> supportedVersions = new HashSet<>();
	static {
		supportedVersions.add("1.0");
	}
	
	private final static Logger LOG = LoggerFactory.getLogger(VersionController.class);
	
	@Autowired
    private ServletContext context;
	
	@RequestMapping(
    		value = {""},
    		method = RequestMethod.POST,
    		consumes = "application/json; charset=UTF-8",
            produces = "application/json; charset=UTF-8")
    @ResponseBody
    public VersionTO version(@RequestBody VersionTO input) {
		final String tag = "{version}";
		final Instant startTime = Instant.now();
		
		try {
			final String serverVersion = getServerVersion();
			final String clientVersion = input.version();
			if (clientVersion == null || clientVersion.isEmpty()) {
				return new VersionTO().type(Type.INPUT_MISMATCH);
			}
			
			final boolean isSupported = isVersionSupported(clientVersion);
			LOG.debug("{} - serverVersion={}, clientVersion={}, isSupported={}", 
					tag, serverVersion, input.version(), isSupported);
			
			return new VersionTO()
					.setSupported(isSupported)
					.setSuccess();
		} catch (Exception e) {
			LOG.error("{} - failed with exception: ", tag, e);
			return new VersionTO()
					.type(Type.SERVER_ERROR)
					.message(e.getMessage());
		} finally {
			LOG.debug("{} - finished in {} ms", tag, Duration.between(startTime, Instant.now()).toMillis());
		}
	}

	private String getServerVersion() throws CoinbleskException {
        try {
        	final String versionKey = "Version";
			InputStream inputStream = context.getResourceAsStream("/META-INF/MANIFEST.MF");
			if (inputStream == null) {
				throw new IOException("Manifest resource not found.");
			}
            Properties prop = new Properties();
            prop.load(inputStream);
            if (!prop.containsKey(versionKey)) {
            	throw new CoinbleskException("Version key not found.");
            }
            return prop.get(versionKey).toString().trim();
        } catch (Exception e) {
        	LOG.error("Could not determine version: ", e);
        	throw new CoinbleskException("Could not determine version.", e);
        }
	}

	private boolean isVersionSupported(String clientVersion) {
		return supportedVersions.contains(clientVersion);
	}
}
