#!/usr/bin/env python3
"""Migrate tool models using the shared vanilla resource converter."""
from base import item
for identifier in ['wrench', 'electric_wrench', 'forge_hammer', 'cutter']:
    item(identifier)
