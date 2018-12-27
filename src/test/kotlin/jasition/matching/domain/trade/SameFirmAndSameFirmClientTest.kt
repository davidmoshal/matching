package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.aFirmWithClient
import jasition.matching.domain.aFirmWithoutClient

internal class SameFirmAndSameFirmClientTest : StringSpec({
    "Different firms without firm client detected" {
        val firm1 = aFirmWithoutClient(firmId = "firm1")
        val firm2 = aFirmWithoutClient(firmId = "firm2")

        sameFirmAndSameFirmClient(firm1, firm2) shouldBe false
        sameFirmAndSameFirmClient(firm2, firm1) shouldBe false
    }
    "Same firm without firm client detected" {
        val firm = aFirmWithoutClient(firmId = "firm1")
        val duplicate = aFirmWithoutClient(firmId = "firm1")

        sameFirmAndSameFirmClient(firm, duplicate) shouldBe true
        sameFirmAndSameFirmClient(duplicate, firm) shouldBe true
    }
    "Different firms, one with firm client and one without detected" {
        val firmWithoutClient = aFirmWithoutClient(firmId = "firm1")
        val firmWithClient = aFirmWithClient(firmId = "firm2")

        sameFirmAndSameFirmClient(firmWithoutClient, firmWithClient) shouldBe false
        sameFirmAndSameFirmClient(firmWithClient, firmWithoutClient) shouldBe false
    }
    "Different firms of different firm clients detected" {
        val firm1 = aFirmWithClient(firmId = "firm1", firmClientId = "client1")
        val firm2 = aFirmWithClient(firmId = "firm2", firmClientId = "client2")

        sameFirmAndSameFirmClient(firm1, firm2) shouldBe false
        sameFirmAndSameFirmClient(firm2, firm1) shouldBe false
    }
    "Same firm, one with firm client and one without detected" {
        val firmWithoutClient = aFirmWithoutClient(firmId = "firm1")
        val firmWithClient = aFirmWithClient(firmId = "firm1")

        sameFirmAndSameFirmClient(firmWithoutClient, firmWithClient) shouldBe false
        sameFirmAndSameFirmClient(firmWithClient, firmWithoutClient) shouldBe false
    }
    "Same firm of different firm clients detected" {
        val firmWithoutClient = aFirmWithClient(firmId = "firm1", firmClientId = "client1")
        val firmWithClient = aFirmWithClient(firmId = "firm1", firmClientId = "client2")

        sameFirmAndSameFirmClient(firmWithoutClient, firmWithClient) shouldBe false
        sameFirmAndSameFirmClient(firmWithClient, firmWithoutClient) shouldBe false
    }
    "Same firm of same firm client detected" {
        val firmWithoutClient = aFirmWithClient(firmId = "firm1", firmClientId = "client1")
        val firmWithClient = aFirmWithClient(firmId = "firm1", firmClientId = "client1")

        sameFirmAndSameFirmClient(firmWithoutClient, firmWithClient) shouldBe true
        sameFirmAndSameFirmClient(firmWithClient, firmWithoutClient) shouldBe true
    }
})