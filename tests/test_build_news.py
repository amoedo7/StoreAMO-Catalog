import unittest

import build_news


class BuildNewsTests(unittest.TestCase):
    def test_public_repo_name(self):
        self.assertEqual(build_news.github_repo_name('https://github.com/amoedo7/GeneralAMO', 'amoedo7'), 'GeneralAMO')
        self.assertIsNone(build_news.github_repo_name('https://desarrollamo.com.ar', 'amoedo7'))

    def test_humanizes_commit_prefixes(self):
        self.assertEqual(build_news.clean_subject('feat: agrega historial')[0], 'feature')
        self.assertEqual(build_news.clean_subject('fix(ui): corrige botón')[0], 'fix')
        self.assertEqual(build_news.clean_subject('release: v0.2.0')[0], 'release')

    def test_private_title_never_needs_commit_text(self):
        self.assertIn('Nueva mejora', build_news.friendly_title('CalculAMO', 'feature'))


if __name__ == '__main__':
    unittest.main()
