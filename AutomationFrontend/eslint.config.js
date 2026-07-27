'use strict';

const js = require('@eslint/js');
const tsParser = require('@typescript-eslint/parser');
const tsPlugin = require('@typescript-eslint/eslint-plugin');
const globals = require('globals');
const prettierConfig = require('eslint-config-prettier');

// Métodos que crean localizadores en los frameworks E2E más comunes
// (Playwright, WebdriverIO/Selenium, DOM nativo). Se usan para bloquear
// su uso directo fuera de los Page Objects.
const LOCATOR_METHODS =
  '^(locator|getByRole|getByText|getByLabel|getByPlaceholder|getByAltText|getByTitle|getByTestId|\\$|\\$\\$|find|findElement|findElements|querySelector|querySelectorAll)$';

// `@typescript-eslint/naming-convention` solo valida declaraciones de campo (class X { foo = 1 }),
// no asignaciones `this.foo = valor` dentro del constructor/métodos — el patrón real que usan
// los Page Objects de este proyecto. Este selector cubre ese hueco: bloquea `this.Propiedad`
// (PascalCase) o `this.mi_propiedad` (snake_case) como propiedades de instancia.
const THIS_PROPERTY_NAMING_RULE = {
  selector:
    "AssignmentExpression[left.object.type='ThisExpression'][left.property.name=/^([A-Z]|.*_)/]",
  message: 'Nomenclatura: las propiedades de clase (this.propiedad) deben ser camelCase.',
};

// Regla POM: bloquea la creación de localizadores. Se reutiliza tanto en Page (test)
// como en Steps — ninguna de las dos puede tocar locators fuera de WebKeywords/pages/.
const NO_LOCATORS_RULE = {
  selector: `CallExpression[callee.property.name=/${LOCATOR_METHODS}/]`,
  message:
    'POM: no se permiten localizadores directos aquí. Encapsúlalos en una clase Page Object (WebKeywords/pages/) y expón un método atómico.',
};
const NO_CYPRESS_LOCATORS_RULE = {
  selector:
    "CallExpression[callee.object.name='cy'][callee.property.name=/^(get|contains|find|xpath)$/]",
  message:
    'POM: no se permiten localizadores directos (cy.get/contains/find/xpath) aquí. Muévelos a un Page Object.',
};

// Fronteras de import entre capas — mismo contrato que Page/Steps/Script en Katalon:
// la dependencia solo va hacia abajo (Test -> Steps -> Page), nunca al revés ni saltando
// un nivel. `require(...)` es un CallExpression normal en CommonJS, así que se detecta
// por el valor literal de su primer argumento (ruta relativa al módulo importado).
function forbidRequireFrom(pathFragmentRegex, message) {
  return {
    selector: `CallExpression[callee.name='require'][arguments.0.value=/${pathFragmentRegex}/]`,
    message,
  };
}

const FORBID_TESTS_IMPORTING_PAGES = forbidRequireFrom(
  '\\/pages\\/',
  'Arquitectura de 3 capas: un test no puede importar un Page Object directamente (WebKeywords/pages/). Debe pasar por la capa Steps (WebKeywords/steps/).'
);
const FORBID_STEPS_IMPORTING_TESTS = forbidRequireFrom(
  '\\/tests\\/',
  'Arquitectura de 3 capas: Steps no puede depender de un archivo de test.'
);
const FORBID_PAGES_IMPORTING_STEPS_OR_TESTS = forbidRequireFrom(
  '\\/(steps|tests)\\/',
  'Arquitectura de 3 capas: Page no puede depender de Steps ni de Tests. La dependencia va solo hacia abajo (Test -> Steps -> Page).'
);

module.exports = [
  {
    ignores: [
      'node_modules/**',
      'bin/**',
      'build/**',
      'Reports/**',
      'output/**',
      '.cache/**',
      // Estructura propia de Katalon Studio (Groovy/XML) — fuera del alcance de este linter JS.
      'Keywords/**',
      'Scripts/**',
      'Test Cases/**',
      'Test Suites/**',
      'Test Listeners/**',
      'Object Repository/**',
      'Include/**',
      'Drivers/**',
      'Plugins/**',
      'Profiles/**',
      'Checkpoints/**',
      'Libs/**',
      'Data Files/**',
      'settings/**',
      'runner/**',
      'runnerTestingTool/**',
      // Scripts legacy del plugin de IA para Appium (fuera del alcance de este proyecto E2E).
      'test_classifier.js',
      'test_classifier_detect.js',
      'tfjs-node-wrapper.js',
    ],
  },

  js.configs.recommended,

  // --- Reglas base para todo archivo JS del proyecto Node.js (WebKeywords/, config, tooling) ---
  {
    files: ['**/*.js'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'commonjs',
      parser: tsParser,
      globals: {
        ...globals.node,
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
    },
    rules: {
      // --- Nomenclatura ---
      '@typescript-eslint/naming-convention': [
        'error',
        // Variables y parámetros por defecto: camelCase.
        { selector: 'variable', format: ['camelCase'] },
        { selector: 'parameter', format: ['camelCase'], leadingUnderscore: 'allow' },
        // Destructuring (const { a, b } = require(...) / obj): el nombre viene de una API
        // externa que no controlamos (módulos, clases importadas, respuestas), así que no se
        // le exige formato. Se define ANTES de las reglas de const/global para que aplique
        // también a imports desestructurados de nivel superior (ver combo de 3 modifiers abajo).
        { selector: 'variable', modifiers: ['destructured'], format: null },
        // Constantes reales (const NO desestructuradas, declaradas en scope global/de módulo):
        // se permite camelCase o UPPER_SNAKE_CASE. No se fuerza solo UPPER_CASE porque a este
        // mismo patrón (const x = require(...)) pertenecen también los imports simples de
        // módulos/utilidades, y no solo los valores constantes en sentido estricto.
        {
          selector: 'variable',
          modifiers: ['const', 'global'],
          format: ['camelCase', 'UPPER_CASE'],
        },
        // Import desestructurado en el top-level del módulo (combina los tres modifiers a la
        // vez): mismo caso que arriba, sin formato forzado.
        {
          selector: 'variable',
          modifiers: ['const', 'global', 'destructured'],
          format: null,
        },
        // Funciones: camelCase.
        { selector: 'function', format: ['camelCase'] },
        // Clases (Page Objects incluidos): PascalCase.
        { selector: 'class', format: ['PascalCase'] },
        { selector: 'classProperty', format: ['camelCase'] },
        { selector: 'classMethod', format: ['camelCase'] },
      ],

      // --- Estructura de scripts: todas las declaraciones al inicio del bloque/función ---
      // "vars-on-top" es la única regla nativa de ESLint que exige esto, y solo aplica a `var`
      // (let/const no se pueden usar antes de declararse por la Temporal Dead Zone, pero ESLint
      // no tiene una regla nativa que fuerce su POSICIÓN al inicio del bloque). Se habilita `var`
      // para las declaraciones de nivel superior de cada función/script y se exige que vayan
      // primero, antes de cualquier lógica de ejecución.
      'vars-on-top': 'error',
      'no-var': 'off',

      'no-restricted-syntax': ['error', THIS_PROPERTY_NAMING_RULE],
    },
  },

  // --- Capa Page (WebKeywords/pages/**): única capa que puede tocar locators. ---
  // No puede depender de Steps ni de Tests (la dependencia va solo hacia abajo).
  {
    files: ['WebKeywords/pages/**/*.js'],
    rules: {
      // Repite THIS_PROPERTY_NAMING_RULE porque los config objects de flat config no hacen
      // merge de arrays por regla: el último que matchea el archivo reemplaza al anterior.
      'no-restricted-syntax': [
        'error',
        THIS_PROPERTY_NAMING_RULE,
        FORBID_PAGES_IMPORTING_STEPS_OR_TESTS,
      ],
    },
  },

  // --- Capa Steps (WebKeywords/steps/**): orquesta Page, expone métodos de negocio. ---
  // No puede tocar locators directo ni depender de Tests.
  {
    files: ['WebKeywords/steps/**/*.js'],
    rules: {
      'no-restricted-syntax': [
        'error',
        THIS_PROPERTY_NAMING_RULE,
        NO_LOCATORS_RULE,
        NO_CYPRESS_LOCATORS_RULE,
        FORBID_STEPS_IMPORTING_TESTS,
      ],
    },
  },

  // --- Capa Test/Script (WebKeywords/tests/**): orquestación delgada. ---
  // Solo puede llamar Steps: ni locators directos ni Page Objects.
  {
    files: ['WebKeywords/tests/**/*.js', '**/*.spec.js', '**/*.test.js'],
    rules: {
      'no-restricted-syntax': [
        'error',
        THIS_PROPERTY_NAMING_RULE,
        NO_LOCATORS_RULE,
        NO_CYPRESS_LOCATORS_RULE,
        FORBID_TESTS_IMPORTING_PAGES,
      ],
    },
  },

  // Debe ir al final: desactiva las reglas de estilo de ESLint que compiten con Prettier.
  prettierConfig,
];
