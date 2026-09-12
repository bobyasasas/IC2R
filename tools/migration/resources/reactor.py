#!/usr/bin/env python3
"""Port functional reactor component items as their heat rules become available."""
import json
from base import OLD, NEW, ASSETS, item, write

components = ['heat_vent', 'rsh_condensator', 'lzh_condensator',
              'uranium_fuel_rod', 'dual_uranium_fuel_rod', 'quad_uranium_fuel_rod',
              'mox_fuel_rod', 'dual_mox_fuel_rod', 'quad_mox_fuel_rod',
              'depleted_uranium_fuel_rod', 'depleted_dual_uranium_fuel_rod',
              'depleted_quad_uranium_fuel_rod', 'depleted_mox_fuel_rod',
              'depleted_dual_mox_fuel_rod', 'depleted_quad_mox_fuel_rod',
              'reactor_heat_vent', 'overclocked_heat_vent', 'advanced_heat_vent',
              'component_heat_vent', 'reactor_coolant_cell', 'triple_reactor_coolant_cell',
              'sextuple_reactor_coolant_cell', 'reactor_plating', 'reactor_heat_plating',
              'containment_reactor_plating', 'heat_exchanger', 'reactor_heat_exchanger',
              'component_heat_exchanger', 'advanced_heat_exchanger', 'neutron_reflector',
              'thick_neutron_reflector', 'iridium_neutron_reflector', 'heatpack',
              'raw_crystal_memory', 'crystal_memory',
              'near_depleted_uranium', 're_enriched_uranium',
              'lithium_fuel_rod', 'depleted_isotope_fuel_rod']
for component in components:
    item(component)
for locale in ['en_us', 'zh_cn']:
    path = ASSETS + 'lang/' + locale + '.json'
    data, old = json.loads((NEW / path).read_text()), json.loads((OLD / path).read_text())
    for component in components:
        data['item.ic2.' + component] = old['item.ic2.' + component]
    write(path, data)
