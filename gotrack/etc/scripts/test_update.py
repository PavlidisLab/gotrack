__author__ = 'mjacobson'
import unittest
import update


class TestSuite(unittest.TestCase):

    def test_search_files_for_go(self):
        import datetime
        files = [f for f in self.files if 'gene_ontology_edit' in f]
        date_map = update.search_files_for_go(files)
        self.assertDictEqual(date_map, {datetime.date(day=1, year=2015, month=3):'/home/xxx/gene_ontology_edit.obo.2015-03-01.gz',
                                        datetime.date(day=1, year=2015, month=4):'/home/xxx/gene_ontology_edit.obo.2015-04-01.gz',
                                        datetime.date(day=1, year=2015, month=5):'/home/xxx/gene_ontology_edit.obo.2015-05-01.gz',
                                        datetime.date(day=1, year=2015, month=6):'/home/xxx/gene_ontology_edit.obo.2015-06-01.gz',
                                        datetime.date(day=1, year=2015, month=7):'/home/xxx/gene_ontology_edit.obo.2015-07-01.gz',
                                        datetime.date(day=1, year=2015, month=8):'/home/xxx/gene_ontology_edit.obo.2015-08-01.gz'})

    def test_search_files_for_goa(self):
        files = [f for f in self.files if 'gene_association.goa' in f]
        species_map = update.search_files_for_goa(files)
        res = {'fly': [
                       (43, '/home/xxx/gene_association.goa_fly.43.gz'),
                       (44, '/home/xxx/gene_association.goa_fly.44.gz'),
                       (45, '/home/xxx/gene_association.goa_fly.45.gz'),
                       (46, '/home/xxx/gene_association.goa_fly.46.gz'),
                       (47, '/home/xxx/gene_association.goa_fly.47.gz')
                       ],
               'cow': [
                       (105, '/home/xxx/gene_association.goa_cow.105.gz'),
                       (106, '/home/xxx/gene_association.goa_cow.106.gz'),
                       (107, '/home/xxx/gene_association.goa_cow.107.gz'),
                       (108, '/home/xxx/gene_association.goa_cow.108.gz'),
                       (109, '/home/xxx/gene_association.goa_cow.109.gz')
                       ],
               'yeast': [
                       (43, '/home/xxx/gene_association.goa_yeast.43.gz'),
                       (44, '/home/xxx/gene_association.goa_yeast.44.gz'),
                       (45, '/home/xxx/gene_association.goa_yeast.45.gz'),
                       (46, '/home/xxx/gene_association.goa_yeast.46.gz'),
                       (47, '/home/xxx/gene_association.goa_yeast.47.gz')
                       ],
               'zebrafish': [
                       (115, '/home/xxx/gene_association.goa_zebrafish.115.gz'),
                       (116, '/home/xxx/gene_association.goa_zebrafish.116.gz'),
                       (117, '/home/xxx/gene_association.goa_zebrafish.117.gz'),
                       (118, '/home/xxx/gene_association.goa_zebrafish.118.gz'),
                       (119, '/home/xxx/gene_association.goa_zebrafish.119.gz')
                       ],
               'dicty': [
                       (43, '/home/xxx/gene_association.goa_dicty.43.gz'),
                       (44, '/home/xxx/gene_association.goa_dicty.44.gz'),
                       (45, '/home/xxx/gene_association.goa_dicty.45.gz'),
                       (46, '/home/xxx/gene_association.goa_dicty.46.gz'),
                       (47, '/home/xxx/gene_association.goa_dicty.47.gz')
                       ],
               'dog': [
                       (43, '/home/xxx/gene_association.goa_dog.43.gz'),
                       (44, '/home/xxx/gene_association.goa_dog.44.gz'),
                       (45, '/home/xxx/gene_association.goa_dog.45.gz'),
                       (46, '/home/xxx/gene_association.goa_dog.46.gz'),
                       (47, '/home/xxx/gene_association.goa_dog.47.gz')
                       ],
               'pig': [
                       (43, '/home/xxx/gene_association.goa_pig.43.gz'),
                       (44, '/home/xxx/gene_association.goa_pig.44.gz'),
                       (45, '/home/xxx/gene_association.goa_pig.45.gz'),
                       (46, '/home/xxx/gene_association.goa_pig.46.gz'),
                       (47, '/home/xxx/gene_association.goa_pig.47.gz')
                       ],
               'arabidopsis': [
                       (115, '/home/xxx/gene_association.goa_arabidopsis.115.gz'),
                       (116, '/home/xxx/gene_association.goa_arabidopsis.116.gz'),
                       (117, '/home/xxx/gene_association.goa_arabidopsis.117.gz'),
                       (118, '/home/xxx/gene_association.goa_arabidopsis.118.gz'),
                       (119, '/home/xxx/gene_association.goa_arabidopsis.119.gz')
                       ],
               'human': [
                       (142, '/home/xxx/gene_association.goa_human.142.gz'),
                       (143, '/home/xxx/gene_association.goa_human.143.gz'),
                       (144, '/home/xxx/gene_association.goa_human.144.gz'),
                       (145, '/home/xxx/gene_association.goa_human.145.gz'),
                       (146, '/home/xxx/gene_association.goa_human.146.gz')
                       ],
               'worm': [
                       (43, '/home/xxx/gene_association.goa_worm.43.gz'),
                       (44, '/home/xxx/gene_association.goa_worm.44.gz'),
                       (45, '/home/xxx/gene_association.goa_worm.45.gz'),
                       (46, '/home/xxx/gene_association.goa_worm.46.gz'),
                       (47, '/home/xxx/gene_association.goa_worm.47.gz')
                       ],
               'chicken': [
                       (112, '/home/xxx/gene_association.goa_chicken.112.gz'),
                       (113, '/home/xxx/gene_association.goa_chicken.113.gz'),
                       (114, '/home/xxx/gene_association.goa_chicken.114.gz'),
                       (115, '/home/xxx/gene_association.goa_chicken.115.gz'),
                       (116, '/home/xxx/gene_association.goa_chicken.116.gz')
                       ],
               'mouse': [
                       (128, '/home/xxx/gene_association.goa_mouse.128.gz'),
                       (129, '/home/xxx/gene_association.goa_mouse.129.gz'),
                       (130, '/home/xxx/gene_association.goa_mouse.130.gz'),
                       (131, '/home/xxx/gene_association.goa_mouse.131.gz'),
                       (132, '/home/xxx/gene_association.goa_mouse.132.gz')
                       ],
               'rat': [
                       (128, '/home/xxx/gene_association.goa_rat.128.gz'),
                       (129, '/home/xxx/gene_association.goa_rat.129.gz'),
                       (130, '/home/xxx/gene_association.goa_rat.130.gz'),
                       (131, '/home/xxx/gene_association.goa_rat.131.gz'),
                       (132, '/home/xxx/gene_association.goa_rat.132.gz')
                       ]}
        self.assertDictEqual(species_map, res)

    def setUp(self):
        self.files = ['/home/xxx/HUMAN_9606_idmapping.dat.gz',
                      '/home/xxx/HUMAN_9606_idmapping_selected.tab.gz',
                      '/home/xxx/acindex.txt',
                      '/home/xxx/download.py',
                      '/home/xxx/gene_association.goa_arabidopsis.115.gz',
                      '/home/xxx/gene_association.goa_arabidopsis.116.gz',
                      '/home/xxx/gene_association.goa_arabidopsis.117.gz',
                      '/home/xxx/gene_association.goa_arabidopsis.118.gz',
                      '/home/xxx/gene_association.goa_arabidopsis.119.gz',
                      '/home/xxx/gene_association.goa_chicken.112.gz',
                      '/home/xxx/gene_association.goa_chicken.113.gz',
                      '/home/xxx/gene_association.goa_chicken.114.gz',
                      '/home/xxx/gene_association.goa_chicken.115.gz',
                      '/home/xxx/gene_association.goa_chicken.116.gz',
                      '/home/xxx/gene_association.goa_cow.105.gz',
                      '/home/xxx/gene_association.goa_cow.106.gz',
                      '/home/xxx/gene_association.goa_cow.107.gz',
                      '/home/xxx/gene_association.goa_cow.108.gz',
                      '/home/xxx/gene_association.goa_cow.109.gz',
                      '/home/xxx/gene_association.goa_dicty.43.gz',
                      '/home/xxx/gene_association.goa_dicty.44.gz',
                      '/home/xxx/gene_association.goa_dicty.45.gz',
                      '/home/xxx/gene_association.goa_dicty.46.gz',
                      '/home/xxx/gene_association.goa_dicty.47.gz',
                      '/home/xxx/gene_association.goa_dog.43.gz',
                      '/home/xxx/gene_association.goa_dog.44.gz',
                      '/home/xxx/gene_association.goa_dog.45.gz',
                      '/home/xxx/gene_association.goa_dog.46.gz',
                      '/home/xxx/gene_association.goa_dog.47.gz',
                      '/home/xxx/gene_association.goa_fly.43.gz',
                      '/home/xxx/gene_association.goa_fly.44.gz',
                      '/home/xxx/gene_association.goa_fly.45.gz',
                      '/home/xxx/gene_association.goa_fly.46.gz',
                      '/home/xxx/gene_association.goa_fly.47.gz',
                      '/home/xxx/gene_association.goa_human.142.gz',
                      '/home/xxx/gene_association.goa_human.143.gz',
                      '/home/xxx/gene_association.goa_human.144.gz',
                      '/home/xxx/gene_association.goa_human.145.gz',
                      '/home/xxx/gene_association.goa_human.146.gz',
                      '/home/xxx/gene_association.goa_mouse.128.gz',
                      '/home/xxx/gene_association.goa_mouse.129.gz',
                      '/home/xxx/gene_association.goa_mouse.130.gz',
                      '/home/xxx/gene_association.goa_mouse.131.gz',
                      '/home/xxx/gene_association.goa_mouse.132.gz',
                      '/home/xxx/gene_association.goa_pig.43.gz',
                      '/home/xxx/gene_association.goa_pig.44.gz',
                      '/home/xxx/gene_association.goa_pig.45.gz',
                      '/home/xxx/gene_association.goa_pig.46.gz',
                      '/home/xxx/gene_association.goa_pig.47.gz',
                      '/home/xxx/gene_association.goa_rat.128.gz',
                      '/home/xxx/gene_association.goa_rat.129.gz',
                      '/home/xxx/gene_association.goa_rat.130.gz',
                      '/home/xxx/gene_association.goa_rat.131.gz',
                      '/home/xxx/gene_association.goa_rat.132.gz',
                      '/home/xxx/gene_association.goa_worm.43.gz',
                      '/home/xxx/gene_association.goa_worm.44.gz',
                      '/home/xxx/gene_association.goa_worm.45.gz',
                      '/home/xxx/gene_association.goa_worm.46.gz',
                      '/home/xxx/gene_association.goa_worm.47.gz',
                      '/home/xxx/gene_association.goa_yeast.43.gz',
                      '/home/xxx/gene_association.goa_yeast.44.gz',
                      '/home/xxx/gene_association.goa_yeast.45.gz',
                      '/home/xxx/gene_association.goa_yeast.46.gz',
                      '/home/xxx/gene_association.goa_yeast.47.gz',
                      '/home/xxx/gene_association.goa_zebrafish.115.gz',
                      '/home/xxx/gene_association.goa_zebrafish.116.gz',
                      '/home/xxx/gene_association.goa_zebrafish.117.gz',
                      '/home/xxx/gene_association.goa_zebrafish.118.gz',
                      '/home/xxx/gene_association.goa_zebrafish.119.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-03-01.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-04-01.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-05-01.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-06-01.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-07-01.gz',
                      '/home/xxx/gene_ontology_edit.obo.2015-08-01.gz',
                      '/home/xxx/idmapping.dat.example',
                      '/home/xxx/idmapping_selected.tab.example',
                      '/home/xxx/sec_ac.txt',
                      '/home/xxx/update.py']

    def tearDown(self):
        self.files = None

if __name__ == '__main__':
    unittest.main()
