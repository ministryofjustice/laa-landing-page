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

    // Tests for null code scenarios
    @Test
    public void testNullCode_ExactNameMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha").code(null).build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testNullCode_CaseInsensitiveNameMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha").code(null).build();
        assertEquals(90, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testNullCode_NameStartsWith() {
        FirmDto firm = FirmDto.builder().name("AlphaTech").code(null).build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testNullCode_NameStartsWithIgnoreCase() {
        FirmDto firm = FirmDto.builder().name("AlphaTech").code(null).build();
        assertEquals(70, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testNullCode_NameContains() {
        FirmDto firm = FirmDto.builder().name("TechAlpha").code(null).build();
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testNullCode_NameContainsIgnoreCase() {
        FirmDto firm = FirmDto.builder().name("TechAlpha").code(null).build();
        assertEquals(50, FirmComparatorByRelevance.relevance(firm, "alpha"));
    }

    @Test
    public void testNullCode_NoMatch() {
        FirmDto firm = FirmDto.builder().name("Beta").code(null).build();
        assertEquals(0, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    // Edge case tests
    @Test
    public void testEmptyQuery() {
        FirmDto firm = FirmDto.builder().name("Alpha").code("ALPHA").build();
        // Empty string matches startsWith, so relevance is 80
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, ""));
    }

    @Test
    public void testEmptyQueryWithNullCode() {
        FirmDto firm = FirmDto.builder().name("Alpha").code(null).build();
        // Empty string matches startsWith, so relevance is 80
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, ""));
    }

    @Test
    public void testQueryLongerThanNameAndCode() {
        FirmDto firm = FirmDto.builder().name("AB").code("CD").build();
        assertEquals(0, FirmComparatorByRelevance.relevance(firm, "ABCDEFGH"));
    }

    @Test
    public void testSpecialCharactersInQuery() {
        FirmDto firm = FirmDto.builder().name("Alpha & Beta").code("A&B").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha & Beta"));
    }

    @Test
    public void testSpecialCharactersInQueryPartialMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha & Beta").code("A&B").build();
        assertEquals(60, FirmComparatorByRelevance.relevance(firm, "& Beta"));
    }

    @Test
    public void testCodeMatchTakesPrecedenceOverName() {
        FirmDto firm = FirmDto.builder().name("Different").code("Alpha").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testNameMatchWhenCodeDoesNotMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha").code("BETA").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha"));
    }

    @Test
    public void testSingleCharacterQuery() {
        FirmDto firm = FirmDto.builder().name("Alpha").code("ALPHA").build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "A"));
    }

    @Test
    public void testSingleCharacterQueryNullCode() {
        FirmDto firm = FirmDto.builder().name("Alpha").code(null).build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "A"));
    }

    @Test
    public void testWhitespaceInQuery() {
        FirmDto firm = FirmDto.builder().name("Alpha Tech").code("ALPHA TECH").build();
        assertEquals(100, FirmComparatorByRelevance.relevance(firm, "Alpha Tech"));
    }

    @Test
    public void testMultipleWordsPartialMatch() {
        FirmDto firm = FirmDto.builder().name("Alpha Beta Gamma").code("ABG").build();
        assertEquals(80, FirmComparatorByRelevance.relevance(firm, "Alpha Beta"));
    }

}