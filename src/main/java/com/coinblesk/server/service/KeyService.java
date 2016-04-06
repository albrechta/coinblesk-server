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

package com.coinblesk.server.service;

import com.coinblesk.server.dao.KeyDAO;
import com.coinblesk.server.entity.Keys;
import com.coinblesk.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Thomas Bocek
 */
@Service
public class KeyService {

    @Autowired
    private KeyDAO clientKeyDAO;
    
    @Transactional(readOnly = true)
    public Keys getByClientPublicKey(final byte[] clientPublicKey) {
        return clientKeyDAO.findByClientPublicKey(clientPublicKey);
    }

    @Transactional(readOnly = true)
    public List<ECKey> getPublicECKeysByClientPublicKey(final byte[] clientPublicKey) {
        final Keys keys = clientKeyDAO.findByClientPublicKey(clientPublicKey);
        final List<ECKey> retVal = new ArrayList<>(2);
        retVal.add(ECKey.fromPublicOnly(keys.clientPublicKey()));
        retVal.add(ECKey.fromPublicOnly(keys.serverPublicKey()));
        return retVal;
    }
    
    @Transactional(readOnly = true)
    public List<ECKey> getECKeysByClientPublicKey(final byte[] clientPublicKey) {
        final Keys keys = clientKeyDAO.findByClientPublicKey(clientPublicKey);
        if(keys == null) {
            return Collections.emptyList();
        }
        final List<ECKey> retVal = new ArrayList<>(2);
        retVal.add(ECKey.fromPublicOnly(keys.clientPublicKey()));
        retVal.add(ECKey.fromPrivateAndPrecalculatedPublic(keys.serverPrivateKey(), keys.serverPublicKey()));
        return retVal;
    }

    @Transactional(readOnly = false)
    public Pair<Boolean, Keys> storeKeysAndAddress(final byte[] clientPublicKey,
            final byte[] serverPublicKey, final byte[] serverPrivateKey) {
        if (clientPublicKey == null || serverPublicKey == null || serverPrivateKey == null ) {
            throw new IllegalArgumentException("null not excpected here");
        }
        
        final Keys clientKey = new Keys()
                .clientPublicKey(clientPublicKey)
                .serverPrivateKey(serverPrivateKey)
                .serverPublicKey(serverPublicKey);

        //need to check if it exists here, as not all DBs does that for us
        final Keys keys = clientKeyDAO.findByClientPublicKey(clientPublicKey);
        if (keys != null) {
            return new Pair<>(false, keys);
        }

        clientKeyDAO.save(clientKey);
        return new Pair<>(true, clientKey);
    }

    @Transactional(readOnly = true)
    public List<List<ECKey>> all() {
        final List<Keys> all = clientKeyDAO.findAll();
        final List<List<ECKey>> retVal = new ArrayList<>();
        for (Keys entity : all) {
            final List<ECKey> keys = new ArrayList<>(2);
            keys.add(ECKey.fromPublicOnly(entity.clientPublicKey()));
            keys.add(ECKey.fromPublicOnly(entity.serverPublicKey()));
            retVal.add(keys);
        }
        return retVal;
    }
}