package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FirmComparatorByRelevanceTest {

    @Test
    public void testExactMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha").code("ALPHA").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha"));
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "ALPHA"));
    }

    @Test
    public void testCaseInsensitiveMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha").code("ALPHA").build();
        assertEquals(90, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testStartsWithMatch() {
        FirmDto firm = FirmDto.builder().name("AlphaTech").code("ALPHACODE").build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "Alpha"));
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "ALPHA"));
    }

    @Test
    public void testStartsWithIgnoreCaseMatch() {
        FirmDto firm = FirmDto.builder().name("AlphaTech").code("ALPHACODE").build();
        assertEquals(70, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testContainsMatch() {
        FirmDto firm = FirmDto.builder().name("TechAlpha").code("CODEALPHA").build();
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "Alpha"));
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "ALPHA"));
    }

    @Test
    public void testContainsIgnoreCaseMatch() {
        FirmDto firm = FirmDto.builder().name("TechAlpha").code("CODEALPHA").build();
        assertEquals(50, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testNoMatch() {
        FirmDto firm = FirmDto.builder().name("Beta").code("BETA123").build();
        assertEquals(0, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testExactMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Alpha Tech").code("ALPHA TECH").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha Tech"));
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "ALPHA TECH"));
    }

    @Test
    public void testCaseInsensitiveMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Alpha Tech").code("ALPHA TECH").build();
        assertEquals(90, FirmComparatorByRelevance.relevance(firm, "alpha tech"));
    }

    @Test
    public void testStartsWithMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Alpha Tech Solutions").code("ALPHA TECH CODE").build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "Alpha Tech"));
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "ALPHA TECH"));
    }

    @Test
    public void testStartsWithIgnoreCaseMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Alpha Tech Solutions").code("ALPHA TECH CODE").build();
        assertEquals(70, FirmComparatorByRelevance.relevance(firm, "alpha tech"));
    }

    @Test
    public void testContainsMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Solutions for Alpha Tech").code("CODE ALPHA TECH").build();
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "Alpha Tech"));
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "ALPHA TECH"));
    }

    @Test
    public void testContainsIgnoreCaseMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Solutions for Alpha Tech").code("CODE ALPHA TECH").build();
        assertEquals(50, FirmComparatorByRelevance.relevance(firm, "alpha tech"));
    }

    @Test
    public void testNoMatchWithSpaces() {
        FirmDto firm = FirmDto.builder().name("Beta Solutions").code("BETA CODE").build();
        assertEquals(0, FirmComparatorByRelevance.relevance(firm, "Alpha Tech"));
    }


    @Test
    public void testExactNumericCodeMatch() {
        FirmDto firm = FirmDto.builder().name("FirmOne").code("12345").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "12345"));
    }

    @Test
    public void testStartsWithNumericCode() {
        FirmDto firm = FirmDto.builder().name("FirmTwo").code("12345").build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "123"));
    }

    @Test
    public void testContainsNumericCode() {
        FirmDto firm = FirmDto.builder().name("FirmThree").code("9912345").build();
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "123"));
    }

    @Test
    public void testNoNumericMatch() {
        FirmDto firm = FirmDto.builder().name("FirmFour").code("98765").build();
        assertEquals(0, FirmComparatorByRelevance.relevance(firm, "123"));
    }

    @Test
    public void testCaseInsensitiveNumericCode() {
        FirmDto firm = FirmDto.builder().name("FirmFive").code("12345").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "12345"));
    }

}