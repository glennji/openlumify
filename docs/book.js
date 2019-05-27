module.exports = {
  title: 'OpenLumify',
  description: 'OpenLumify, a big data analysis and visualization platform to help analysts and investigators solve ambiguous problems.',
  gitbook: '3.2.x',
  language: 'en',
  direction: 'ltr',
  // UPDATE Makefile "plugins=" variable if changing
  plugins: [ '-sharing', 'ga', 'theme-openlumify', 'github-embed' ],
  styles: {
      website: 'styles/website.css'
  },
  pluginsConfig: {
    ga: {
        token: 'UA-63006144-4',
        configuration: {
            cookieDomain: 'docs.openlumify.org'
        }
    },
    lunr: {
        maxIndexSize: 1000000000
    },
    'theme-openlumify': {
        canonicalBaseUrl: 'http://docs.openlumify.org'
    }
  }
};
