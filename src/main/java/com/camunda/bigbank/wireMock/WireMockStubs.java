package com.camunda.bigbank.wireMock;

import org.springframework.stereotype.Service;

import com.camunda.bigbank.entities.Customer;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;

@Service
public class WireMockStubs 
{
    WireMockServer wireMockServer;

    public WireMockStubs()
    {
        wireMockServer = new WireMockServer(8089);
        registerStubs();
        wireMockServer.start();
    }

    private void registerStubs(){
        wireMockServer.stubFor(get("/hello-world").willReturn(ok("Hi!")));
        wireMockServer.stubFor(get("/bigbank").willReturn(ok("where we're so big, even our piggy banks have their own personal assistants. If size mattered in banking – wait, it does!")));        
    }

    public void registerOfferStubs(Customer customer){
        String customerName = (customer.getFirstName() + customer.getLastName()).replaceAll("\\s", "");

        //BKR rating
        String bkrUrl = "/RequestBKR_" + customerName;
        String bkrResponse = "{BKR:{name:" + customerName + ", rating:positive}}";

        //Land registry
        String customerAddress = customer.getAddress().replaceAll("\\s", "");
        String landRegistryUrl = "/LandRegistry_" + customerAddress;
        String landRegistryResponse = "{\"LandOwnership\":{\"address\":\"" + customerAddress + "\", \"ownership\":\"" + customerName + "\"}}";

        wireMockServer.stubFor(get(bkrUrl).willReturn(ok(bkrResponse)));
        wireMockServer.stubFor(get(landRegistryUrl).willReturn(ok(landRegistryResponse)));
    }

    @Override
    protected void finalize() {
        wireMockServer.stop();
    }
}