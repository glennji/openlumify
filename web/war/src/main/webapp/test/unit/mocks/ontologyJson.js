define(['text!/base/test/unit/mocks/ontology.json'], function(json) {
    // Hack ontology for testing
    var ontologyJson = JSON.parse(json),
        name = _.findWhere(ontologyJson.properties, { id: 'http://openlumify.org/dev#name' }),
        person = _.findWhere(ontologyJson.concepts, { id: 'http://openlumify.org/dev#person' });

    // Delete color for person
    if (person) {
        delete person.color;
    }

    ontologyJson.properties.push({
      'title': 'http://openlumify.org/dev#nameNoValidationFormula',
      'displayName': 'Name',
      'userVisible': true,
      'searchable': true,
      'dataType': 'string',
      'dependentPropertyIris': [
        'http://openlumify.org/dev#lastName',
        'http://openlumify.org/dev#firstName'
      ]
    });

    var first = _.findWhere(ontologyJson.properties, { title: 'http://openlumify.org/dev#firstName' })
    first.validationFormula = "propRaw('http://openlumify.org/dev#firstName') && propRaw('http://openlumify.org/dev#firstName').length > 1";
    var last = _.findWhere(ontologyJson.properties, { title: 'http://openlumify.org/dev#lastName' })
    last.validationFormula = "propRaw('http://openlumify.org/dev#lastName') && propRaw('http://openlumify.org/dev#lastName').length > 2";

    // Add compound field that dependends on another compound
    ontologyJson.properties.push({
        title: 'http://openlumify.org/testing#compound1',
        displayName: 'Testing Compound',
        userVisible: true,
        searchable: true,
        dataType: 'string',
        validationFormula:
            'dependentProp("http://openlumify.org/dev#title") && ' +
            'dependentProp("http://openlumify.org/dev#name")',
        displayFormula:
            'dependentProp("http://openlumify.org/dev#name") + ", "' +
            'dependentProp("http://openlumify.org/dev#title")',
        dependentPropertyIris: [
            'http://openlumify.org/dev#title',
            'http://openlumify.org/dev#name'
        ]
    })

    // Add heading
    ontologyJson.properties.push({
        title: 'http://openlumify.org/testing#heading1',
        displayName: 'Testing Heading',
        userVisible: true,
        searchable: true,
        dataType: 'double',
        displayType: 'heading'
    })

    ontologyJson.properties.push({
        title: 'http://openlumify.org/testing#integer1',
        displayName: 'Testing integer',
        userVisible: true,
        searchable: true,
        dataType: 'integer',
        validationFormula: 'propRaw("http://openlumify.org/testing#integer1") > 1'
    })

    ontologyJson.properties.push({
        title: 'http://openlumify.org/testing#integer1noform',
        displayName: 'Testing integer',
        userVisible: true,
        searchable: true,
        dataType: 'integer'
    })

    ontologyJson.properties.push({
        title: 'http://openlumify.org/testing#number1',
        displayName: 'Testing number',
        userVisible: true,
        searchable: true,
        dataType: 'number'
    })

    // Add video sub concept to test displayType
    ontologyJson.concepts.push({
        id:'http://openlumify.org/dev#videoSub',
        title:'http://openlumify.org/dev#videoSub',
        displayName:'VideoSub',
        parentConcept:'http://openlumify.org/dev#video',
        pluralDisplayName:'VideoSubs',
        searchable:true,
        properties:[]
    });

    ontologyJson.concepts.push({
        id:'http://openlumify.org/dev#personSub',
        title:'http://openlumify.org/dev#personSub',
        displayName:'PersonSub',
        parentConcept:'http://openlumify.org/dev#person',
        pluralDisplayName:'PersonSubs',
        searchable:true,
        properties:[]
    });

    ontologyJson.concepts.push({
        id:'http://openlumify.org/dev#formulaTestConcept',
        title:'http://openlumify.org/dev#formulaTestConcept',
        displayName:'FormulaTestConcept',
        parentConcept:'http://openlumify.org#root',
        pluralDisplayName:'FormulaTestConcepts',
        titleFormula: 'ontology.displayName',
        properties:[]
    })

    ontologyJson.concepts.push({
        id:'http://openlumify.org/dev#formulaTest',
        title:'http://openlumify.org/dev#formulaTest',
        displayName:'FormulaTest',
        parentConcept:'http://openlumify.org#root',
        pluralDisplayName:'FormulaTests',
        titleFormula: `String(
            typeof prop === "function" &&
            typeof propRaw === "function" &&
            typeof longestProp === "function" &&
            typeof ontology === "object" &&
            typeof vertex === 'object' &&
            typeof edge === 'undefined'
        )`,
        properties:[]
    })

    return ontologyJson;
});
